package com.example.natureaura.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.natureaura.connectivity.WearableBiometricBridge
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.BleSensorDevice
import com.example.natureaura.model.CalibrationPreset
import com.example.natureaura.model.SensorCapabilityReport
import com.example.natureaura.model.TelemetrySourceMode
import com.example.natureaura.model.WearableNodeInfo
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryBorder
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuarySurface
import com.example.ui.theme.SanctuaryVoid
import com.example.ui.theme.WarmAmber

@Composable
fun WearablesScreen(
    sourceMode: TelemetrySourceMode,
    wearOsAvailable: Boolean,
    connectedWearNodes: List<WearableNodeInfo>,
    discoveredBleDevices: List<BleSensorDevice>,
    isBleScanning: Boolean,
    statusMessage: String,
    activePreset: CalibrationPreset?,
    sensorCapabilities: SensorCapabilityReport,
    activeSensorSummary: String,
    latestPayload: BiometricPayload?,
    onEnableOnDeviceSensors: () -> Unit,
    onRefreshWearNodes: () -> Unit,
    onStartBleScan: () -> Unit,
    onStopBleScan: () -> Unit,
    onConnectBleDevice: (String) -> Unit,
    onToggleCameraPpg: (Boolean) -> Unit,
    onSelectCalibrationPreset: (CalibrationPreset) -> Unit,
    onStopCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )
    }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            onStartBleScan()
        }
    }

    val cameraPpgActive = sourceMode == TelemetrySourceMode.CAMERA_PPG

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SanctuaryForest, SanctuaryVoid)))
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "Wearable Connectivity Bridge",
                    style = MaterialTheme.typography.headlineLarge,
                    color = IvoryPrimary,
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sourceMode == TelemetrySourceMode.CALIBRATION_SIGNAL) WarmAmber else CeladonAccent,
                )
            }

            // On-Device Zero-Preset Phone Sensors & Fingerprint Trigger Card
            item {
                val phoneActive = sourceMode == TelemetrySourceMode.ON_DEVICE_SENSORS
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (phoneActive) CeladonAccent else SanctuaryBorder),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = CeladonAccent)
                            Column {
                                Text(
                                    text = "Phone Sensors · Zero-Preset Offline Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryPrimary,
                                )
                                Text(
                                    text = activeSensorSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CeladonAccent,
                                )
                            }
                        }

                        Text(
                            text = buildString {
                                append("Front Camera rPPG extracts live pulse; SensorManager reads stillness & ambient temperature")
                                if (sensorCapabilities.hasFingerprintHardware) {
                                    append("; Hardware Fingerprint Scanner is present and triggers tactile capture (TEE blocks direct pulse/temp readout from fingerprint chips).")
                                } else {
                                    append(".")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SageMuted,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CapabilityChip(
                                label = if (sensorCapabilities.isFingerprintEnrolled) "Fingerprint Ready" else if (sensorCapabilities.hasFingerprintHardware) "FP No Print" else "No FP HW",
                                active = sensorCapabilities.hasFingerprintHardware,
                            )
                            CapabilityChip(
                                label = "Accel",
                                active = sensorCapabilities.hasAccelerometer,
                            )
                            CapabilityChip(
                                label = "Temp",
                                active = sensorCapabilities.hasAmbientTempSensor,
                            )
                            CapabilityChip(
                                label = "Light",
                                active = sensorCapabilities.hasLightSensor,
                            )
                        }

                        Button(
                            onClick = onEnableOnDeviceSensors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("activate_phone_sensors_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CeladonAccent,
                                contentColor = SanctuaryVoid,
                            ),
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (phoneActive) "Phone Sensors Active — Go to Scan" else "Use Phone Sensors (No Preset)")
                        }
                    }
                }
            }

            // Wear OS Data Layer API Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SanctuaryBorder),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Watch, contentDescription = null, tint = CeladonAccent)
                                Column {
                                    Text(
                                        text = "Wear OS Data Layer (MessageClient)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IvoryPrimary,
                                    )
                                    Text(
                                        text = "Endpoint: ${WearableBiometricBridge.PATH_BIOMETRICS}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SageMuted,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onRefreshWearNodes,
                                modifier = Modifier.height(48.dp),
                                border = BorderStroke(1.dp, SanctuaryBorder),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh nodes", tint = IvoryPrimary)
                            }
                        }

                        if (!wearOsAvailable) {
                            Text(
                                text = "Google Play Services Wearable API not present on this runtime.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SageMuted,
                            )
                        } else if (connectedWearNodes.isEmpty()) {
                            Text(
                                text = "Background WearableListenerService armed. Pair a Wear OS watch streaming HEART_RATE_CONTINUOUS, SkinTemperature, and EDA.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SageMuted,
                            )
                        } else {
                            connectedWearNodes.forEach { node ->
                                Surface(
                                    color = SanctuaryVoid,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(node.displayName, color = IvoryPrimary)
                                        Text(
                                            text = if (node.isNearby) "Nearby" else "Cloud Node",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CeladonAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Phone Rear-Camera Finger PPG Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (cameraPpgActive) CeladonAccent else SanctuaryBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = CeladonAccent)
                            Column {
                                Text(
                                    text = "Phone Camera Finger-PPG (No Watch Needed)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryPrimary,
                                )
                                Text(
                                    text = "Uses rear camera + flash optical luma peaks to measure real BPM & RR-SDNN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SageMuted,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onToggleCameraPpg(!cameraPpgActive) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("camera_ppg_toggle_button"),
                            border = BorderStroke(1.dp, CeladonAccent),
                        ) {
                            Text(
                                text = if (cameraPpgActive) "Stop Camera PPG" else "Activate Camera Finger-PPG",
                                color = CeladonAccent,
                            )
                        }
                    }
                }
            }

            // Standard BLE GATT Heart Rate Service Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SanctuaryBorder),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = CeladonAccent)
                            Column {
                                Text(
                                    text = "BLE Heart Rate & RR-SDNN (0x180D)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryPrimary,
                                )
                                Text(
                                    text = "Direct GATT subscription for chest straps, rings, & BLE watches",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SageMuted,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isBleScanning) {
                                    onStopBleScan()
                                } else {
                                    blePermissionLauncher.launch(blePermissions)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ble_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CeladonAccent,
                                contentColor = SanctuaryVoid,
                            ),
                        ) {
                            Text(if (isBleScanning) "Stop BLE Scan" else "Scan for BLE Biometric Sensors")
                        }

                        discoveredBleDevices.forEach { device ->
                            Surface(
                                color = SanctuaryVoid,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (device.isConnected) CeladonAccent else SanctuaryBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, color = IvoryPrimary)
                                        Text(
                                            text = "${device.address} · ${device.rssi} dBm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SageMuted,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onConnectBleDevice(device.address) },
                                        modifier = Modifier.height(48.dp),
                                    ) {
                                        Text(if (device.isConnected) "Connected" else "Connect")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Explicit Calibration Signal Generator
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Diagnostic Calibration Presets",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmAmber,
                            )
                            if (activePreset != null) {
                                OutlinedButton(
                                    onClick = onStopCalibration,
                                    modifier = Modifier.height(48.dp),
                                ) {
                                    Text("Stop", color = WarmAmber)
                                }
                            }
                        }

                        CalibrationPreset.entries.forEach { preset ->
                            val isSelected = activePreset == preset
                            Surface(
                                color = if (isSelected) WarmAmber.copy(alpha = 0.14f) else SanctuaryVoid,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) WarmAmber else SanctuaryBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = IvoryPrimary,
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SageMuted,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { onSelectCalibrationPreset(preset) },
                                        modifier = Modifier.height(48.dp),
                                    ) {
                                        Text(if (isSelected) "Active" else "Stream", color = WarmAmber)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (latestPayload != null) {
                item {
                    Surface(
                        color = SanctuaryVoid,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SanctuaryBorder),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                text = "LIVE BIOMETRIC JSON PAYLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = CeladonAccent,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = latestPayload.toJsonString(),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvoryPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(
    label: String,
    active: Boolean,
) {
    Surface(
        color = if (active) CeladonAccent.copy(alpha = 0.16f) else SanctuaryVoid,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (active) CeladonAccent.copy(alpha = 0.55f) else SanctuaryBorder),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) CeladonAccent else SageMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
