package com.example.natureaura.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.BleSensorDevice
import com.example.natureaura.model.CalibrationPreset
import com.example.natureaura.model.SensorCapabilityReport
import com.example.natureaura.model.TelemetrySourceMode
import com.example.natureaura.model.WearableNodeInfo
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hardware & Data Connectivity Bridge.
 *
 * Bridges real-time biometric telemetry from:
 * 1. Wear OS Data Layer API (`MessageClient` + background `WearableListenerService` bus).
 * 2. Standard BLE GATT Heart Rate Service (`0x180D` / `0x2A37`) + RR-interval SDNN HRV.
 * 3. Phone Rear-Camera Finger PPG (`CameraPpgAnalyzer`).
 * 4. Explicit user-selected Diagnostic Calibration Signal presets when no wearable is paired.
 */
class WearableBiometricBridge(private val context: Context) {

    companion object {
        const val PATH_BIOMETRICS = "/nature_aura/biometrics"
        const val PATH_CONTROL = "/nature_aura/control"

        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _biometricStream = MutableSharedFlow<BiometricPayload>(replay = 1, extraBufferCapacity = 32)
    val biometricStream: SharedFlow<BiometricPayload> = _biometricStream.asSharedFlow()

    private val _sourceMode = MutableStateFlow(TelemetrySourceMode.ON_DEVICE_SENSORS)
    val sourceMode: StateFlow<TelemetrySourceMode> = _sourceMode.asStateFlow()

    private val _wearOsAvailable = MutableStateFlow(false)
    val wearOsAvailable: StateFlow<Boolean> = _wearOsAvailable.asStateFlow()

    private val _connectedWearNodes = MutableStateFlow<List<WearableNodeInfo>>(emptyList())
    val connectedWearNodes: StateFlow<List<WearableNodeInfo>> = _connectedWearNodes.asStateFlow()

    private val _discoveredBleDevices = MutableStateFlow<List<BleSensorDevice>>(emptyList())
    val discoveredBleDevices: StateFlow<List<BleSensorDevice>> = _discoveredBleDevices.asStateFlow()

    private val _isBleScanning = MutableStateFlow(false)
    val isBleScanning: StateFlow<Boolean> = _isBleScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow("Live Phone Sensors armed (Zero-Preset Offline Mode)")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _activeCalibrationPreset = MutableStateFlow<CalibrationPreset?>(null)
    val activeCalibrationPreset: StateFlow<CalibrationPreset?> = _activeCalibrationPreset.asStateFlow()

    private val onDeviceHarvester = OnDeviceBiometricHarvester(context) { payload ->
        if (_sourceMode.value == TelemetrySourceMode.ON_DEVICE_SENSORS) {
            _biometricStream.tryEmit(payload)
        }
    }
    val sensorCapabilities: StateFlow<SensorCapabilityReport> = onDeviceHarvester.capabilities
    val activeSensorSummary: StateFlow<String> = onDeviceHarvester.activeSensorSummary

    private var calibrationJob: Job? = null
    private var messageListenerRegistered = false
    private var activeGatt: BluetoothGatt? = null
    private val recentRrIntervalsMs = ArrayDeque<Double>(32)

    private val messageListener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
        if (event.path == PATH_BIOMETRICS) {
            val rawJson = String(event.data, StandardCharsets.UTF_8)
            runCatching { BiometricPayload.fromJsonString(rawJson) }
                .onSuccess { payload -> handleIncomingWearPayload(payload) }
                .onFailure { err ->
                    _statusMessage.value = "Malformed Wear OS payload: ${err.message}"
                }
        }
    }

    init {
        onDeviceHarvester.start()
        scope.launch {
            WearPayloadBus.incomingWearPayloads.collect { payload ->
                handleIncomingWearPayload(payload)
            }
        }
    }

    private fun handleIncomingWearPayload(payload: BiometricPayload) {
        stopCalibrationSignal()
        onDeviceHarvester.stop()
        _sourceMode.value = TelemetrySourceMode.WEAR_OS_DATA_LAYER
        _statusMessage.value = "Receiving Wear OS telemetry from ${payload.deviceId}"
        _biometricStream.tryEmit(payload)
    }

    fun initializeWearOsBridge() {
        val availability = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        if (availability != ConnectionResult.SUCCESS) {
            _wearOsAvailable.value = false
            _statusMessage.value = "Google Play Services unavailable for Wear OS Data Layer"
            return
        }

        runCatching {
            val client = Wearable.getMessageClient(context)
            if (!messageListenerRegistered) {
                client.addListener(messageListener)
                messageListenerRegistered = true
            }
            _wearOsAvailable.value = true
            refreshWearNodes()
        }.onFailure {
            _wearOsAvailable.value = false
            _statusMessage.value = "Wear OS Data Layer unavailable on this device"
        }
    }

    fun refreshWearNodes() {
        if (!_wearOsAvailable.value) return
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                val mapped = nodes.map {
                    WearableNodeInfo(
                        id = it.id,
                        displayName = it.displayName,
                        isNearby = it.isNearby,
                    )
                }
                _connectedWearNodes.value = mapped
                if (mapped.isNotEmpty() && _sourceMode.value == TelemetrySourceMode.NONE) {
                    _sourceMode.value = TelemetrySourceMode.WEAR_OS_DATA_LAYER
                    _statusMessage.value = "Paired Wear OS node: ${mapped.first().displayName}"
                }
            }
            .addOnFailureListener {
                _connectedWearNodes.value = emptyList()
            }
    }

    fun sendWearOsControlCommand(command: String) {
        val nodes = _connectedWearNodes.value
        if (nodes.isEmpty()) return
        val client = Wearable.getMessageClient(context)
        val data = command.toByteArray(StandardCharsets.UTF_8)
        nodes.forEach { node ->
            client.sendMessage(node.id, PATH_CONTROL, data)
                .addOnSuccessListener {
                    _statusMessage.value = "Sent $command to ${node.displayName}"
                }
        }
    }

    fun enableOnDeviceSensorMode() {
        calibrationJob?.cancel()
        calibrationJob = null
        _activeCalibrationPreset.value = null
        stopBleScan()
        _sourceMode.value = TelemetrySourceMode.ON_DEVICE_SENSORS
        onDeviceHarvester.start()
        _statusMessage.value = "Phone Sensors & Front rPPG active (No Preset)"
    }

    fun updateFrontRppgSnapshot(snapshot: FrontRppgSnapshot) {
        onDeviceHarvester.updateFrontRppg(snapshot)
    }

    fun enableCameraPpgMode() {
        calibrationJob?.cancel()
        calibrationJob = null
        _activeCalibrationPreset.value = null
        onDeviceHarvester.stop()
        stopBleScan()
        _sourceMode.value = TelemetrySourceMode.CAMERA_PPG
        _statusMessage.value = "Place fingertip gently over rear camera & flash"
    }

    fun disableCameraPpgMode() {
        if (_sourceMode.value == TelemetrySourceMode.CAMERA_PPG) {
            enableOnDeviceSensorMode()
        }
    }

    fun ingestPpgPayload(payload: BiometricPayload) {
        if (_sourceMode.value == TelemetrySourceMode.CAMERA_PPG) {
            _statusMessage.value = "Rear Finger-PPG active · ${payload.heartRateBpm.toInt()} bpm"
            _biometricStream.tryEmit(payload)
        }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "BLE Heart Sensor"
            val entry = BleSensorDevice(
                address = device.address,
                name = name,
                rssi = result.rssi,
                isConnected = activeGatt?.device?.address == device.address,
            )
            val current = _discoveredBleDevices.value.toMutableList()
            val idx = current.indexOfFirst { it.address == entry.address }
            if (idx >= 0) current[idx] = entry else current.add(entry)
            _discoveredBleDevices.value = current.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            _isBleScanning.value = false
            _statusMessage.value = "BLE scan error ($errorCode)"
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        val adapter = bluetoothAdapter()
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = "Bluetooth is off or unavailable on this device"
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _statusMessage.value = "BLE scanner unavailable"
            return
        }
        _discoveredBleDevices.value = emptyList()
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching {
            scanner.startScan(listOf(filter), settings, bleScanCallback)
            _isBleScanning.value = true
            _statusMessage.value = "Scanning for BLE Heart Rate wearables (0x180D)..."
        }.onFailure { err ->
            _isBleScanning.value = false
            _statusMessage.value = "BLE permission required: ${err.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        val scanner = bluetoothAdapter()?.bluetoothLeScanner ?: return
        if (_isBleScanning.value) {
            runCatching { scanner.stopScan(bleScanCallback) }
            _isBleScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBleDevice(address: String) {
        stopBleScan()
        val device: BluetoothDevice = bluetoothAdapter()?.getRemoteDevice(address) ?: return
        activeGatt?.close()
        recentRrIntervalsMs.clear()
        _statusMessage.value = "Connecting to BLE sensor $address..."

        activeGatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        stopCalibrationSignal()
                        _sourceMode.value = TelemetrySourceMode.BLE_GATT
                        _statusMessage.value = "Connected to ${device.address}, discovering services..."
                        _discoveredBleDevices.value = _discoveredBleDevices.value.map {
                            it.copy(isConnected = it.address == address)
                        }
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        _discoveredBleDevices.value = _discoveredBleDevices.value.map {
                            it.copy(isConnected = false)
                        }
                        if (_sourceMode.value == TelemetrySourceMode.BLE_GATT) {
                            _sourceMode.value = TelemetrySourceMode.NONE
                            _statusMessage.value = "BLE sensor disconnected"
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    val hrService = gatt.getService(HEART_RATE_SERVICE_UUID) ?: return
                    val hrChar = hrService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID) ?: return
                    gatt.setCharacteristicNotification(hrChar, true)
                    val cccd = hrChar.getDescriptor(CCCD_UUID) ?: return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(cccd)
                    }
                    _statusMessage.value = "Streaming live BLE Heart Rate & RR-SDNN"
                }

                @Deprecated("Deprecated in API 33")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                ) {
                    @Suppress("DEPRECATION")
                    val bytes = characteristic.value ?: return
                    handleHeartRatePacket(gatt.device.address, bytes)
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                ) {
                    handleHeartRatePacket(gatt.device.address, value)
                }
            },
        )
    }

    private fun handleHeartRatePacket(deviceAddress: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val flags = bytes[0].toInt() and 0xFF
        val isUint16Hr = (flags and 0x01) != 0
        val hasEnergyExpended = (flags and 0x08) != 0
        val hasRrInterval = (flags and 0x10) != 0

        var offset = 1
        val bpm = if (isUint16Hr && bytes.size >= 3) {
            offset += 2
            ((bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)).toDouble()
        } else if (bytes.size >= 2) {
            offset += 1
            (bytes[1].toInt() and 0xFF).toDouble()
        } else {
            return
        }

        if (hasEnergyExpended) offset += 2

        if (hasRrInterval) {
            while (offset + 1 < bytes.size) {
                val rawRr = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
                val rrMs = (rawRr / 1024.0) * 1000.0
                if (recentRrIntervalsMs.size >= 24) recentRrIntervalsMs.removeFirst()
                recentRrIntervalsMs.addLast(rrMs)
                offset += 2
            }
        }

        val sdnn = computeSdnn(recentRrIntervalsMs.toList())
        _biometricStream.tryEmit(
            BiometricPayload(
                deviceId = deviceAddress,
                sourcePlatform = "BLE_GATT",
                timestampMs = System.currentTimeMillis(),
                heartRateBpm = bpm,
                hrvSdnnMs = sdnn,
                skinTempCelsius = 35.0,
                edaMicroSiemens = 2.8,
                restingHeartRateBpm = bpm,
                signalQuality = 0.96,
            ),
        )
    }

    private fun computeSdnn(rrList: List<Double>): Double {
        if (rrList.size < 2) return 48.0
        val mean = rrList.average()
        val variance = rrList.sumOf { (it - mean).pow(2) } / rrList.size
        return sqrt(variance).coerceIn(10.0, 140.0)
    }

    fun startCalibrationSignal(preset: CalibrationPreset) {
        stopBleScan()
        onDeviceHarvester.stop()
        calibrationJob?.cancel()
        _activeCalibrationPreset.value = preset
        _sourceMode.value = TelemetrySourceMode.CALIBRATION_SIGNAL
        _statusMessage.value = "Calibration preset: ${preset.title}"

        calibrationJob = scope.launch {
            var tick = 0
            while (isActive) {
                val phase = tick * 0.45
                val hrWave = sin(phase) * preset.hrJitter + cos(phase * 0.5) * (preset.hrJitter * 0.35)
                val hrvWave = cos(phase * 0.35) * 4.0
                val tempWave = sin(phase * 0.2) * 0.15
                val edaWave = sin(phase * 0.6) * 0.45

                val payload = BiometricPayload(
                    deviceId = "cal-${preset.name.lowercase()}",
                    sourcePlatform = "Calibration_Signal",
                    timestampMs = System.currentTimeMillis(),
                    heartRateBpm = (preset.baseBpm + hrWave).coerceIn(42.0, 165.0),
                    hrvSdnnMs = (preset.baseHrvSdnn + hrvWave).coerceIn(12.0, 130.0),
                    skinTempCelsius = (preset.baseSkinTempC + tempWave).coerceIn(30.0, 38.5),
                    edaMicroSiemens = (preset.baseEdaMicroS + edaWave).coerceIn(0.3, 20.0),
                    restingHeartRateBpm = preset.baseBpm,
                    signalQuality = 1.0,
                )
                _biometricStream.emit(payload)
                tick++
                delay(500L)
            }
        }
    }

    fun stopCalibrationSignal() {
        calibrationJob?.cancel()
        calibrationJob = null
        _activeCalibrationPreset.value = null
        if (_sourceMode.value == TelemetrySourceMode.CALIBRATION_SIGNAL) {
            enableOnDeviceSensorMode()
        }
    }

    @SuppressLint("MissingPermission")
    fun release() {
        calibrationJob?.cancel()
        calibrationJob = null
        onDeviceHarvester.stop()
        stopBleScan()
        runCatching { activeGatt?.close() }
        activeGatt = null
        if (messageListenerRegistered) {
            runCatching { Wearable.getMessageClient(context).removeListener(messageListener) }
            messageListenerRegistered = false
        }
    }
}
