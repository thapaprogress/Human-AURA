package com.example.natureaura.ui

import android.app.Application
import android.content.Intent
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.natureaura.audio.FrequencyTherapyService
import com.example.natureaura.audio.TherapyForegroundService
import com.example.natureaura.connectivity.CameraPpgAnalyzer
import com.example.natureaura.connectivity.FrontCameraRppgAnalyzer
import com.example.natureaura.connectivity.WearableBiometricBridge
import com.example.natureaura.data.AuraReadingRepository
import com.example.natureaura.data.BirthChartPreferences
import com.example.natureaura.engine.AuraCalculator
import com.example.natureaura.engine.AuraReadingRecord
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.BirthChartProfile
import com.example.natureaura.model.BleSensorDevice
import com.example.natureaura.model.BreathPaceMode
import com.example.natureaura.model.CalibrationPreset
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.PersonalBaseline
import com.example.natureaura.model.SensorCapabilityReport
import com.example.natureaura.model.SleepTimerOption
import com.example.natureaura.model.TelemetrySourceMode
import com.example.natureaura.model.WearableNodeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanPhaseState {
    data object Idle : ScanPhaseState
    data class Scanning(
        val elapsedSeconds: Int,
        val totalSeconds: Int = 15,
        val progress: Float,
        val collectedSamples: Int,
    ) : ScanPhaseState
    data class Completed(
        val result: AuraResult,
        val savedRecord: AuraReadingRecord? = null,
    ) : ScanPhaseState
    data class Error(val message: String) : ScanPhaseState
}

data class AuraUiState(
    val latestPayload: BiometricPayload? = null,
    val liveAura: AuraResult? = null,
    val scanPhase: ScanPhaseState = ScanPhaseState.Idle,
    val lastCompletedScan: AuraResult? = null,
    val isSavingPhoto: Boolean = false,
    val pendingParentReadingId: Long? = null,
    val breathPaceMode: BreathPaceMode = BreathPaceMode.COHERENT_5_5,
    val fingerprintNotice: String? = null,
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val SCAN_WINDOW_SECONDS = 15
    }

    private val calculator = AuraCalculator()
    private val repository = AuraReadingRepository(application.applicationContext)
    private val birthChartPreferences = BirthChartPreferences(application.applicationContext)
    val bridge = WearableBiometricBridge(application.applicationContext)
    val therapyService = FrequencyTherapyService()

    val cameraPpgAnalyzer = CameraPpgAnalyzer { payload ->
        bridge.ingestPpgPayload(payload)
    }

    val frontRppgAnalyzer = FrontCameraRppgAnalyzer { snapshot ->
        bridge.updateFrontRppgSnapshot(snapshot)
    }

    private val _uiState = MutableStateFlow(AuraUiState())
    val uiState: StateFlow<AuraUiState> = _uiState.asStateFlow()

    val savedReadings: StateFlow<List<AuraReadingRecord>> = repository.allReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personalBaseline: StateFlow<PersonalBaseline?> = repository.personalBaseline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val birthChartProfile: StateFlow<BirthChartProfile?> = birthChartPreferences.birthProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedReading = MutableStateFlow<AuraReadingRecord?>(null)
    val selectedReading: StateFlow<AuraReadingRecord?> = _selectedReading.asStateFlow()

    val sourceMode: StateFlow<TelemetrySourceMode> = bridge.sourceMode
    val wearOsAvailable: StateFlow<Boolean> = bridge.wearOsAvailable
    val connectedWearNodes: StateFlow<List<WearableNodeInfo>> = bridge.connectedWearNodes
    val discoveredBleDevices: StateFlow<List<BleSensorDevice>> = bridge.discoveredBleDevices
    val isBleScanning: StateFlow<Boolean> = bridge.isBleScanning
    val connectionStatus: StateFlow<String> = bridge.statusMessage
    val activeCalibrationPreset: StateFlow<CalibrationPreset?> = bridge.activeCalibrationPreset
    val sensorCapabilities: StateFlow<SensorCapabilityReport> = bridge.sensorCapabilities
    val activeSensorSummary: StateFlow<String> = bridge.activeSensorSummary

    val activeBand: StateFlow<FrequencyBand> = therapyService.activeBand
        .stateIn(viewModelScope, SharingStarted.Eagerly, FrequencyBand.AUM)
    val secondaryBand: StateFlow<FrequencyBand?> = therapyService.secondaryBand
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isAudioPlaying: StateFlow<Boolean> = therapyService.isPlaying
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val audioVolume: StateFlow<Float> = therapyService.volume
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.65f)
    val audioElapsedSeconds: StateFlow<Int> = therapyService.elapsedSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val sleepTimerOption: StateFlow<SleepTimerOption> = therapyService.sleepTimerOption
        .stateIn(viewModelScope, SharingStarted.Eagerly, SleepTimerOption.CONTINUOUS)

    private val rollingLiveBuffer = ArrayDeque<BiometricPayload>(12)
    private val scanWindowBuffer = mutableListOf<BiometricPayload>()
    private var scanTimerJob: Job? = null
    private var activeImageCapture: ImageCapture? = null

    init {
        bridge.initializeWearOsBridge()
        observeBiometricStream()
    }

    fun updateImageCapture(imageCapture: ImageCapture?) {
        activeImageCapture = imageCapture
    }

    private fun observeBiometricStream() {
        viewModelScope.launch {
            bridge.biometricStream.collect { payload ->
                if (rollingLiveBuffer.size >= 10) rollingLiveBuffer.removeFirst()
                rollingLiveBuffer.addLast(payload)

                val liveCalculated = calculator.calculate(
                    samples = rollingLiveBuffer.toList(),
                    baseline = personalBaseline.value,
                )
                val isScanning = _uiState.value.scanPhase is ScanPhaseState.Scanning
                if (isScanning) {
                    scanWindowBuffer.add(payload)
                }

                _uiState.update { state ->
                    state.copy(
                        latestPayload = payload,
                        liveAura = liveCalculated,
                    )
                }
            }
        }
    }

    fun startPostTherapyFollowUp(parentReadingId: Long?) {
        _uiState.update { it.copy(pendingParentReadingId = parentReadingId) }
        _selectedReading.value = null
    }

    fun setFingerprintNotice(message: String?) {
        _uiState.update { it.copy(fingerprintNotice = message) }
    }

    fun enableOnDeviceSensorMode() {
        bridge.enableOnDeviceSensorMode()
    }

    fun saveBirthChartProfile(month: Int, day: Int, hour24: Int) {
        viewModelScope.launch {
            birthChartPreferences.saveProfile(BirthChartProfile(month, day, hour24))
        }
    }

    fun clearBirthChartProfile() {
        viewModelScope.launch {
            birthChartPreferences.clearProfile()
        }
    }

    fun startAuraScan() {
        if (bridge.sourceMode.value == TelemetrySourceMode.NONE) {
            bridge.enableOnDeviceSensorMode()
        }

        scanTimerJob?.cancel()
        scanWindowBuffer.clear()
        _uiState.value.latestPayload?.let { scanWindowBuffer.add(it) }

        bridge.sendWearOsControlCommand("START_SCAN")

        scanTimerJob = viewModelScope.launch {
            val totalTicks = SCAN_WINDOW_SECONDS * 10
            for (tick in 0..totalTicks) {
                val progress = tick.toFloat() / totalTicks.toFloat()
                val elapsedSec = tick / 10
                _uiState.update {
                    it.copy(
                        scanPhase = ScanPhaseState.Scanning(
                            elapsedSeconds = elapsedSec,
                            totalSeconds = SCAN_WINDOW_SECONDS,
                            progress = progress,
                            collectedSamples = scanWindowBuffer.size,
                        ),
                    )
                }
                delay(100L)
            }

            bridge.sendWearOsControlCommand("STOP_SCAN")

            if (scanWindowBuffer.isEmpty()) {
                _uiState.update {
                    it.copy(
                        scanPhase = ScanPhaseState.Error(
                            "No biometric samples received during the 15s window. Check camera/sensor access.",
                        ),
                    )
                }
            } else {
                val baseline = repository.getBaselineOnce()
                val finalAura = calculator.calculate(scanWindowBuffer.toList(), baseline)
                val sourcePlatform = scanWindowBuffer.last().sourcePlatform
                val parentId = _uiState.value.pendingParentReadingId
                _uiState.update { it.copy(isSavingPhoto = true) }

                val savedRecord = runCatching {
                    repository.saveCaptureAndReading(
                        result = finalAura,
                        sourcePlatform = sourcePlatform,
                        imageCapture = activeImageCapture,
                        parentReadingId = parentId,
                    )
                }.getOrNull()

                _uiState.update {
                    it.copy(
                        scanPhase = ScanPhaseState.Completed(finalAura, savedRecord),
                        lastCompletedScan = finalAura,
                        liveAura = finalAura,
                        isSavingPhoto = false,
                        pendingParentReadingId = null,
                    )
                }
            }
        }
    }

    fun captureInstantAuraPhoto() {
        val currentResult = _uiState.value.liveAura ?: _uiState.value.lastCompletedScan
        if (currentResult == null) {
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhaseState.Error(
                        "Hold steady for a second while Phone Sensors / rPPG gather your initial baseline.",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPhoto = true) }
            val sourcePlatform = _uiState.value.latestPayload?.sourcePlatform ?: sourceMode.value.displayName
            val parentId = _uiState.value.pendingParentReadingId
            val savedRecord = runCatching {
                repository.saveCaptureAndReading(
                    result = currentResult,
                    sourcePlatform = sourcePlatform,
                    imageCapture = activeImageCapture,
                    parentReadingId = parentId,
                )
            }.getOrNull()

            _uiState.update {
                it.copy(
                    scanPhase = ScanPhaseState.Completed(currentResult, savedRecord),
                    lastCompletedScan = currentResult,
                    isSavingPhoto = false,
                    pendingParentReadingId = null,
                )
            }
        }
    }

    fun createShareIntent(record: AuraReadingRecord): Intent? {
        val uri = repository.getShareablePhotoUri(record.photoPath) ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Nature Aura — ${record.narrative.archetypeTitle} (${(record.result.overallBalance * 100).toInt()}% Harmony)",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun resetPersonalBaseline() {
        viewModelScope.launch { repository.resetBaseline() }
    }

    fun openReadingDetail(record: AuraReadingRecord) {
        _selectedReading.value = record
    }

    fun closeReadingDetail() {
        _selectedReading.value = null
    }

    fun deleteReading(record: AuraReadingRecord) {
        viewModelScope.launch {
            repository.deleteReading(record)
            if (_selectedReading.value?.id == record.id) {
                _selectedReading.value = null
            }
        }
    }

    fun cancelAuraScan() {
        scanTimerJob?.cancel()
        scanTimerJob = null
        bridge.sendWearOsControlCommand("STOP_SCAN")
        _uiState.update { it.copy(scanPhase = ScanPhaseState.Idle, pendingParentReadingId = null) }
    }

    fun dismissScanStatus() {
        _uiState.update { it.copy(scanPhase = ScanPhaseState.Idle, fingerprintNotice = null) }
    }

    fun startCalibrationPreset(preset: CalibrationPreset) {
        bridge.startCalibrationSignal(preset)
    }

    fun stopCalibrationSignal() {
        cancelAuraScan()
        bridge.stopCalibrationSignal()
        rollingLiveBuffer.clear()
        _uiState.update { it.copy(latestPayload = null, liveAura = null) }
    }

    fun toggleCameraPpgMode(enable: Boolean) {
        if (enable) bridge.enableCameraPpgMode() else bridge.disableCameraPpgMode()
    }

    fun startBleScan() = bridge.startBleScan()
    fun stopBleScan() = bridge.stopBleScan()
    fun connectBleDevice(address: String) = bridge.connectBleDevice(address)
    fun refreshWearNodes() = bridge.refreshWearNodes()

    fun setBreathPaceMode(mode: BreathPaceMode) {
        _uiState.update { it.copy(breathPaceMode = mode) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        therapyService.setSleepTimer(option)
    }

    fun launchRecommendedTherapy(resultOverride: AuraResult? = null) {
        val result = resultOverride ?: _uiState.value.lastCompletedScan ?: _uiState.value.liveAura
        if (result != null) {
            therapyService.play(
                primary = result.prescription.primaryBand,
                secondary = result.prescription.secondaryBand,
            )
        } else {
            therapyService.play(FrequencyBand.AUM, FrequencyBand.ALPHA)
        }
        TherapyForegroundService.syncForegroundService(getApplication(), true)
    }

    fun selectAndPlayBand(band: FrequencyBand) {
        therapyService.play(primary = band, secondary = null)
        TherapyForegroundService.syncForegroundService(getApplication(), true)
    }

    fun toggleAudioPlayPause() {
        therapyService.togglePlayPause()
        TherapyForegroundService.syncForegroundService(
            getApplication(),
            therapyService.isPlaying.value,
        )
    }

    fun setAudioVolume(volume: Float) {
        therapyService.setVolume(volume)
    }

    override fun onCleared() {
        super.onCleared()
        scanTimerJob?.cancel()
        bridge.release()
        therapyService.release()
        TherapyForegroundService.syncForegroundService(getApplication(), false)
    }
}
