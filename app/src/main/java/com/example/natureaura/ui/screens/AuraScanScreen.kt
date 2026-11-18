package com.example.natureaura.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.natureaura.connectivity.CameraPpgAnalyzer
import com.example.natureaura.connectivity.FingerprintTriggerHelper
import com.example.natureaura.connectivity.FrontCameraRppgAnalyzer
import com.example.natureaura.engine.AuraReadingRecord
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.CalibrationPreset
import com.example.natureaura.model.SensorCapabilityReport
import com.example.natureaura.model.TelemetrySourceMode
import com.example.natureaura.ui.AuraUiState
import com.example.natureaura.ui.ScanPhaseState
import com.example.natureaura.ui.components.AuraShaderOverlay
import com.example.natureaura.ui.components.FrontCameraPreview
import com.example.natureaura.ui.components.LocalAuraPhotoImage
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.CeladonDeep
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryBorder
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuarySurface
import com.example.ui.theme.SanctuaryVoid
import com.example.ui.theme.WarmAmber
import java.util.Locale

@Composable
fun AuraScanScreen(
    uiState: AuraUiState,
    sourceMode: TelemetrySourceMode,
    activePreset: CalibrationPreset?,
    activeSensorSummary: String,
    sensorCapabilities: SensorCapabilityReport,
    ppgAnalyzer: CameraPpgAnalyzer,
    frontRppgAnalyzer: FrontCameraRppgAnalyzer,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onStartScan: () -> Unit,
    onSnapInstantPhoto: () -> Unit,
    onCancelScan: () -> Unit,
    onDismissStatus: () -> Unit,
    onEnableOnDeviceSensors: () -> Unit,
    onEnableCameraPpg: () -> Unit,
    onDisableCameraPpg: () -> Unit,
    onFingerprintNotice: (String?) -> Unit,
    onSelectCalibrationPreset: (CalibrationPreset) -> Unit,
    onStopCalibration: () -> Unit,
    onOpenReadingDetail: (AuraReadingRecord) -> Unit,
    onOpenChakras: () -> Unit,
    onLaunchTherapy: () -> Unit,
    onOpenWearables: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    val scanPhase = uiState.scanPhase
    val isScanning = scanPhase is ScanPhaseState.Scanning
    val isCameraPpgActive = sourceMode == TelemetrySourceMode.CAMERA_PPG
    var showPresets by remember(sourceMode) {
        mutableStateOf(sourceMode == TelemetrySourceMode.CALIBRATION_SIGNAL)
    }

    Box(modifier = modifier.fillMaxSize().background(SanctuaryVoid)) {
        // 1. Live Front-Facing CameraX Feed + ImageCapture + Front rPPG (or Rear Finger-PPG mode)
        FrontCameraPreview(
            hasCameraPermission = hasCameraPermission,
            isCameraPpgActive = isCameraPpgActive,
            ppgAnalyzer = ppgAnalyzer,
            frontRppgAnalyzer = frontRppgAnalyzer,
            onImageCaptureReady = onImageCaptureReady,
        )

        // 2. Dynamic AGSL / Radial Aura Shader Overlay bound to AuraCalculator
        AuraShaderOverlay(
            auraResult = uiState.liveAura,
            heartRateBpm = uiState.latestPayload?.heartRateBpm,
            isScanning = isScanning,
        )

        // 3. Silhouette alignment & spectral 15-second capture ring
        val sortedLive = uiState.liveAura?.chakras?.sortedByDescending { it.intensity }
        SilhouetteCaptureReticle(
            scanPhase = scanPhase,
            accentColor = sortedLive?.getOrNull(0)?.chakra?.color ?: CeladonAccent,
            secondaryColor = sortedLive?.getOrNull(1)?.chakra?.color ?: WarmAmber,
        )

        // 4. Live Data HUD (Top + Bottom Controls)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TopHudHeader(
                    sourceMode = sourceMode,
                    onOpenWearables = onOpenWearables,
                )

                if (uiState.pendingParentReadingId != null) {
                    Surface(
                        color = CeladonAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            text = "POST-THERAPY DELTA SCAN ARMED — Measure Before vs. After Shift",
                            style = MaterialTheme.typography.labelSmall,
                            color = CeladonAccent,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                if (sourceMode == TelemetrySourceMode.CALIBRATION_SIGNAL) {
                    CalibrationNoticeBanner(
                        preset = activePreset,
                        onStop = onStopCalibration,
                    )
                } else if (isCameraPpgActive) {
                    Surface(
                        color = CeladonAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "CAMERA PPG: Cover rear lens & flash gently with fingertip",
                                style = MaterialTheme.typography.labelSmall,
                                color = CeladonAccent,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Exit",
                                style = MaterialTheme.typography.labelSmall,
                                color = IvoryPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onDisableCameraPpg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                } else if (sourceMode == TelemetrySourceMode.ON_DEVICE_SENSORS) {
                    Surface(
                        color = SanctuaryForest.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "LIVE SENSORS (NO PRESET): $activeSensorSummary",
                            style = MaterialTheme.typography.labelSmall,
                            color = CeladonAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                uiState.fingerprintNotice?.let { notice ->
                    Surface(
                        color = WarmAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.labelSmall,
                                color = IvoryPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = { onFingerprintNotice(null) })
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                LiveBiometricHudRow(payload = uiState.latestPayload)
            }

            if (!hasCameraPermission) {
                CameraPermissionBanner(
                    onRequestPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.widthIn(max = 460.dp),
                )
            }

            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(
                    visible = scanPhase is ScanPhaseState.Completed,
                    enter = fadeIn() + slideInVertically { it / 3 },
                    exit = fadeOut(),
                ) {
                    val completed = scanPhase as? ScanPhaseState.Completed
                    if (completed != null) {
                        ScanCompletedSummaryCard(
                            completed = completed,
                            onLaunchTherapy = onLaunchTherapy,
                            onViewReading = {
                                completed.savedRecord?.let(onOpenReadingDetail) ?: onOpenChakras()
                            },
                            onDismiss = onDismissStatus,
                        )
                    }
                }

                if (scanPhase is ScanPhaseState.Error) {
                    Surface(
                        color = CoralAlert.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = scanPhase.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvoryPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onDismissStatus) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss message",
                                    tint = IvoryPrimary,
                                )
                            }
                        }
                    }
                }

                if (!isScanning) {
                    OfflineSensorQuickRow(
                        sourceMode = sourceMode,
                        hasFingerprintHardware = sensorCapabilities.hasFingerprintHardware,
                        showPresets = showPresets,
                        onSelectPhoneSensors = {
                            onFingerprintNotice(null)
                            onEnableOnDeviceSensors()
                        },
                        onTriggerFingerprintScan = {
                            FingerprintTriggerHelper.triggerFingerprintScan(
                                context = context,
                                onVerified = {
                                    onFingerprintNotice("Fingerprint confirmed — live Phone Sensor & rPPG scan started.")
                                    onEnableOnDeviceSensors()
                                    onStartScan()
                                },
                                onFallbackDirectScan = { reason ->
                                    onFingerprintNotice(reason)
                                    onEnableOnDeviceSensors()
                                    onStartScan()
                                },
                            )
                        },
                        onToggleFingerPpg = {
                            if (isCameraPpgActive) onDisableCameraPpg() else onEnableCameraPpg()
                        },
                        onTogglePresets = { showPresets = !showPresets },
                    )
                }

                if (showPresets || sourceMode == TelemetrySourceMode.CALIBRATION_SIGNAL) {
                    CalibrationPresetStrip(
                        activePreset = activePreset,
                        onSelectPreset = onSelectCalibrationPreset,
                    )
                }

                Surface(
                    color = SanctuaryForest.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SanctuaryBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (isScanning) {
                            val scanning = scanPhase as ScanPhaseState.Scanning
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.scanning_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryPrimary,
                                )
                                Text(
                                    text = "${15 - scanning.elapsedSeconds}s remaining · ${scanning.collectedSamples} frames",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CeladonAccent,
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelScan,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cancel_scan_button"),
                                border = BorderStroke(1.dp, CoralAlert),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = CoralAlert,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.stop_scan), color = CoralAlert)
                            }
                        } else {
                            Button(
                                onClick = onStartScan,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("start_scan_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CeladonAccent,
                                    contentColor = SanctuaryVoid,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isSavingPhoto) {
                                        "Composing Aura Photo..."
                                    } else {
                                        stringResource(R.string.start_scan)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onSnapInstantPhoto,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("snap_aura_photo_button"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = CeladonDeep,
                                    contentColor = IvoryPrimary,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = stringResource(R.string.snap_aura_photo),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHudHeader(
    sourceMode: TelemetrySourceMode,
    onOpenWearables: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Nature Aura",
                style = MaterialTheme.typography.headlineMedium,
                color = IvoryPrimary,
            )
            Text(
                text = "OFFLINE BIOFEEDBACK · AURA PHOTO",
                style = MaterialTheme.typography.labelSmall,
                color = SageMuted,
            )
        }

        val badgeColor = when (sourceMode) {
            TelemetrySourceMode.ON_DEVICE_SENSORS -> CeladonAccent
            TelemetrySourceMode.WEAR_OS_DATA_LAYER -> CeladonAccent
            TelemetrySourceMode.BLE_GATT -> CeladonAccent
            TelemetrySourceMode.CAMERA_PPG -> CeladonAccent
            TelemetrySourceMode.CALIBRATION_SIGNAL -> WarmAmber
            TelemetrySourceMode.NONE -> SageMuted
        }

        Surface(
            color = SanctuaryForest.copy(alpha = 0.84f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenWearables)
                .testTag("wearable_status_chip"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = when (sourceMode) {
                        TelemetrySourceMode.WEAR_OS_DATA_LAYER -> Icons.Default.Watch
                        TelemetrySourceMode.BLE_GATT -> Icons.Default.BluetoothConnected
                        TelemetrySourceMode.CAMERA_PPG -> Icons.Default.Favorite
                        else -> Icons.Default.Sensors
                    },
                    contentDescription = "Wearable source status",
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = sourceMode.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CalibrationNoticeBanner(
    preset: CalibrationPreset?,
    onStop: () -> Unit,
) {
    Surface(
        color = WarmAmber.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "CALIBRATION SIGNAL: ${preset?.title ?: "Active"} (No physical wearable)",
                style = MaterialTheme.typography.labelSmall,
                color = WarmAmber,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Stop",
                style = MaterialTheme.typography.labelSmall,
                color = IvoryPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onStop)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LiveBiometricHudRow(payload: BiometricPayload?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudMetricPill(
            label = "BPM",
            value = payload?.let { "${it.heartRateBpm.toInt()}" } ?: "--",
            accent = CeladonAccent,
            modifier = Modifier.weight(1f),
        )
        HudMetricPill(
            label = "TEMP",
            value = payload?.let { String.format(Locale.US, "%.1f°C", it.skinTempCelsius) } ?: "--",
            accent = WarmAmber,
            modifier = Modifier.weight(1f),
        )
        HudMetricPill(
            label = "HRV",
            value = payload?.let { "${it.hrvSdnnMs.toInt()}ms" } ?: "--",
            accent = CeladonAccent,
            modifier = Modifier.weight(1f),
        )
        HudMetricPill(
            label = "EDA",
            value = payload?.let { String.format(Locale.US, "%.1fµS", it.edaMicroSiemens) } ?: "--",
            accent = WarmAmber,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HudMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SanctuaryForest.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SanctuaryBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SageMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SilhouetteCaptureReticle(
    scanPhase: ScanPhaseState,
    accentColor: Color,
    secondaryColor: Color,
) {
    val progress = (scanPhase as? ScanPhaseState.Scanning)?.progress ?: 0f
    val isScanning = scanPhase is ScanPhaseState.Scanning

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.45f)
        val radius = size.minDimension * 0.31f

        drawCircle(
            color = accentColor.copy(alpha = if (isScanning) 0.34f else 0.22f),
            radius = radius,
            center = center,
            style = Stroke(width = 6.dp.toPx()),
        )
        drawCircle(
            color = IvoryPrimary.copy(alpha = if (isScanning) 0.42f else 0.20f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        if (isScanning) {
            drawArc(
                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                    colors = listOf(accentColor, secondaryColor, accentColor),
                    center = center,
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun CameraPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SanctuaryBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CeladonAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraFront,
                    contentDescription = null,
                    tint = CeladonAccent,
                )
            }
            Text(
                text = stringResource(R.string.camera_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = IvoryPrimary,
            )
            Text(
                text = stringResource(R.string.camera_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SageMuted,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_camera_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CeladonAccent,
                    contentColor = SanctuaryVoid,
                ),
            ) {
                Text(stringResource(R.string.grant_camera))
            }
        }
    }
}

@Composable
private fun OfflineSensorQuickRow(
    sourceMode: TelemetrySourceMode,
    hasFingerprintHardware: Boolean,
    showPresets: Boolean,
    onSelectPhoneSensors: () -> Unit,
    onTriggerFingerprintScan: () -> Unit,
    onToggleFingerPpg: () -> Unit,
    onTogglePresets: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val phoneSelected = sourceMode == TelemetrySourceMode.ON_DEVICE_SENSORS
        FilterChip(
            selected = phoneSelected,
            onClick = onSelectPhoneSensors,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = { Text("Phone Scan", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("mode_phone_sensors_chip"),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SanctuaryForest.copy(alpha = 0.88f),
                labelColor = IvoryPrimary,
                iconColor = SageMuted,
                selectedContainerColor = CeladonAccent.copy(alpha = 0.24f),
                selectedLabelColor = CeladonAccent,
                selectedLeadingIconColor = CeladonAccent,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = phoneSelected,
                borderColor = SanctuaryBorder,
                selectedBorderColor = CeladonAccent,
            ),
        )

        FilterChip(
            selected = false,
            onClick = onTriggerFingerprintScan,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = {
                Text(
                    text = if (hasFingerprintHardware) "Fingerprint Start" else "Touch Start",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            modifier = Modifier.testTag("fingerprint_scan_chip"),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SanctuaryForest.copy(alpha = 0.88f),
                labelColor = IvoryPrimary,
                iconColor = CeladonAccent,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = CeladonAccent.copy(alpha = 0.55f),
            ),
        )

        val ppgSelected = sourceMode == TelemetrySourceMode.CAMERA_PPG
        FilterChip(
            selected = ppgSelected,
            onClick = onToggleFingerPpg,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = { Text("Finger-PPG", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("mode_finger_ppg_chip"),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SanctuaryForest.copy(alpha = 0.88f),
                labelColor = IvoryPrimary,
                iconColor = SageMuted,
                selectedContainerColor = CeladonAccent.copy(alpha = 0.24f),
                selectedLabelColor = CeladonAccent,
                selectedLeadingIconColor = CeladonAccent,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = ppgSelected,
                borderColor = SanctuaryBorder,
                selectedBorderColor = CeladonAccent,
            ),
        )

        val presetActive = showPresets || sourceMode == TelemetrySourceMode.CALIBRATION_SIGNAL
        FilterChip(
            selected = presetActive,
            onClick = onTogglePresets,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = { Text("Presets", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("toggle_presets_chip"),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SanctuaryForest.copy(alpha = 0.88f),
                labelColor = SageMuted,
                iconColor = SageMuted,
                selectedContainerColor = WarmAmber.copy(alpha = 0.22f),
                selectedLabelColor = WarmAmber,
                selectedLeadingIconColor = WarmAmber,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = presetActive,
                borderColor = SanctuaryBorder,
                selectedBorderColor = WarmAmber,
            ),
        )
    }
}

@Composable
private fun CalibrationPresetStrip(
    activePreset: CalibrationPreset?,
    onSelectPreset: (CalibrationPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Optional Diagnostic Calibration Presets",
            style = MaterialTheme.typography.labelSmall,
            color = WarmAmber,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalibrationPreset.entries.forEach { preset ->
                val selected = activePreset == preset
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.title, style = MaterialTheme.typography.bodyMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SanctuaryForest.copy(alpha = 0.85f),
                        labelColor = IvoryPrimary,
                        selectedContainerColor = WarmAmber.copy(alpha = 0.25f),
                        selectedLabelColor = WarmAmber,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SanctuaryBorder,
                        selectedBorderColor = WarmAmber,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScanCompletedSummaryCard(
    completed: ScanPhaseState.Completed,
    onLaunchTherapy: () -> Unit,
    onViewReading: () -> Unit,
    onDismiss: () -> Unit,
) {
    val result = completed.result
    val saved = completed.savedRecord

    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, result.dominantChakra.chakra.color.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (saved?.photoPath != null) {
                    LocalAuraPhotoImage(
                        photoPath = saved.photoPath,
                        contentDescription = "Captured Aura Photograph preview",
                        modifier = Modifier
                            .size(width = 58.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onViewReading),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aura Photo Saved Offline · ${(result.overallBalance * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = CeladonAccent,
                    )
                    Text(
                        text = saved?.narrative?.archetypeTitle ?: result.dominantChakra.chakra.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary,
                    )
                    Text(
                        text = "Weakest: ${result.weakestChakra.chakra.englishName} · Outliers filtered: ${result.filteredOutlierCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageMuted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close summary", tint = SageMuted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onViewReading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("view_reading_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CeladonAccent,
                        contentColor = SanctuaryVoid,
                    ),
                ) {
                    Text("Aura Reading")
                }
                OutlinedButton(
                    onClick = onLaunchTherapy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("launch_therapy_button"),
                    border = BorderStroke(1.dp, result.prescription.primaryBand.accentColor),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = result.prescription.primaryBand.accentColor,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = result.prescription.primaryBand.frequencyLabel,
                        color = result.prescription.primaryBand.accentColor,
                    )
                }
            }
        }
    }
}
