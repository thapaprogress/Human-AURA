package com.example.natureaura.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.natureaura.ui.screens.AuraJournalScreen
import com.example.natureaura.ui.screens.AuraReadingDetailScreen
import com.example.natureaura.ui.screens.AuraScanScreen
import com.example.natureaura.ui.screens.ChakraBreakdownScreen
import com.example.natureaura.ui.screens.TherapyScreen
import com.example.natureaura.ui.screens.WearablesScreen
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.CeladonDeep
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuaryVoid

enum class NatureAuraRoute(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    SCAN("aura_scan", R.string.nav_scan, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    CHAKRAS("chakras", R.string.nav_chakras, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    JOURNAL("journal", R.string.nav_journal, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    THERAPY("therapy", R.string.nav_therapy, Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    WEARABLES("wearables", R.string.nav_wearables, Icons.Filled.Watch, Icons.Outlined.Watch),
}

@Composable
fun NatureAuraApp(
    viewModel: AuraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NatureAuraRoute.SCAN.route

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedReadings by viewModel.savedReadings.collectAsStateWithLifecycle()
    val personalBaseline by viewModel.personalBaseline.collectAsStateWithLifecycle()
    val birthChartProfile by viewModel.birthChartProfile.collectAsStateWithLifecycle()
    val selectedReading by viewModel.selectedReading.collectAsStateWithLifecycle()

    val sourceMode by viewModel.sourceMode.collectAsStateWithLifecycle()
    val wearOsAvailable by viewModel.wearOsAvailable.collectAsStateWithLifecycle()
    val connectedWearNodes by viewModel.connectedWearNodes.collectAsStateWithLifecycle()
    val discoveredBleDevices by viewModel.discoveredBleDevices.collectAsStateWithLifecycle()
    val isBleScanning by viewModel.isBleScanning.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val activePreset by viewModel.activeCalibrationPreset.collectAsStateWithLifecycle()
    val sensorCapabilities by viewModel.sensorCapabilities.collectAsStateWithLifecycle()
    val activeSensorSummary by viewModel.activeSensorSummary.collectAsStateWithLifecycle()

    val activeBand by viewModel.activeBand.collectAsStateWithLifecycle()
    val secondaryBand by viewModel.secondaryBand.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
    val audioElapsedSeconds by viewModel.audioElapsedSeconds.collectAsStateWithLifecycle()
    val sleepTimerOption by viewModel.sleepTimerOption.collectAsStateWithLifecycle()

    fun navigateTo(route: NatureAuraRoute) {
        viewModel.closeReadingDetail()
        navController.navigate(route.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SanctuaryVoid,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail && selectedReading == null) {
                    NavigationBar(
                        containerColor = SanctuaryForest,
                        contentColor = IvoryPrimary,
                    ) {
                        NatureAuraRoute.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CeladonAccent,
                                    selectedTextColor = CeladonAccent,
                                    indicatorColor = CeladonDeep.copy(alpha = 0.65f),
                                    unselectedIconColor = SageMuted,
                                    unselectedTextColor = SageMuted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail && selectedReading == null) {
                    NavigationRail(
                        containerColor = SanctuaryForest,
                        contentColor = IvoryPrimary,
                    ) {
                        NatureAuraRoute.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("rail_${dest.route}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CeladonAccent,
                                    selectedTextColor = CeladonAccent,
                                    indicatorColor = CeladonDeep.copy(alpha = 0.65f),
                                    unselectedIconColor = SageMuted,
                                    unselectedTextColor = SageMuted,
                                ),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    val activeDetail = selectedReading
                    if (activeDetail != null) {
                        val parentRecord = activeDetail.parentReadingId?.let { pid ->
                            savedReadings.firstOrNull { it.id == pid }
                        }
                        AuraReadingDetailScreen(
                            record = activeDetail,
                            parentRecord = parentRecord,
                            birthChartProfile = birthChartProfile,
                            onSaveBirthChartProfile = viewModel::saveBirthChartProfile,
                            onClearBirthChartProfile = viewModel::clearBirthChartProfile,
                            onBack = viewModel::closeReadingDetail,
                            onShare = { rec ->
                                viewModel.createShareIntent(rec)?.let { intent ->
                                    context.startActivity(Intent.createChooser(intent, "Share Aura Photograph"))
                                }
                            },
                            onDelete = viewModel::deleteReading,
                            onLaunchTherapy = { result ->
                                viewModel.closeReadingDetail()
                                viewModel.launchRecommendedTherapy(result)
                                navigateTo(NatureAuraRoute.THERAPY)
                            },
                            onStartDeltaFollowUp = { parentId ->
                                viewModel.startPostTherapyFollowUp(parentId)
                                navigateTo(NatureAuraRoute.SCAN)
                            },
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = NatureAuraRoute.SCAN.route,
                        ) {
                            composable(NatureAuraRoute.SCAN.route) {
                                AuraScanScreen(
                                    uiState = uiState,
                                    sourceMode = sourceMode,
                                    activePreset = activePreset,
                                    activeSensorSummary = activeSensorSummary,
                                    sensorCapabilities = sensorCapabilities,
                                    ppgAnalyzer = viewModel.cameraPpgAnalyzer,
                                    frontRppgAnalyzer = viewModel.frontRppgAnalyzer,
                                    onImageCaptureReady = viewModel::updateImageCapture,
                                    onStartScan = viewModel::startAuraScan,
                                    onSnapInstantPhoto = viewModel::captureInstantAuraPhoto,
                                    onCancelScan = viewModel::cancelAuraScan,
                                    onDismissStatus = viewModel::dismissScanStatus,
                                    onEnableOnDeviceSensors = viewModel::enableOnDeviceSensorMode,
                                    onEnableCameraPpg = { viewModel.toggleCameraPpgMode(true) },
                                    onDisableCameraPpg = { viewModel.toggleCameraPpgMode(false) },
                                    onFingerprintNotice = viewModel::setFingerprintNotice,
                                    onSelectCalibrationPreset = viewModel::startCalibrationPreset,
                                    onStopCalibration = viewModel::stopCalibrationSignal,
                                    onOpenReadingDetail = viewModel::openReadingDetail,
                                    onOpenChakras = { navigateTo(NatureAuraRoute.CHAKRAS) },
                                    onLaunchTherapy = {
                                        viewModel.launchRecommendedTherapy()
                                        navigateTo(NatureAuraRoute.THERAPY)
                                    },
                                    onOpenWearables = { navigateTo(NatureAuraRoute.WEARABLES) },
                                )
                            }

                            composable(NatureAuraRoute.CHAKRAS.route) {
                                ChakraBreakdownScreen(
                                    auraResult = uiState.lastCompletedScan ?: uiState.liveAura,
                                    onStartScanTab = { navigateTo(NatureAuraRoute.SCAN) },
                                    onLaunchTherapy = {
                                        viewModel.launchRecommendedTherapy()
                                        navigateTo(NatureAuraRoute.THERAPY)
                                    },
                                )
                            }

                            composable(NatureAuraRoute.JOURNAL.route) {
                                AuraJournalScreen(
                                    readings = savedReadings,
                                    baseline = personalBaseline,
                                    birthChartProfile = birthChartProfile,
                                    onSaveBirthChartProfile = viewModel::saveBirthChartProfile,
                                    onClearBirthChartProfile = viewModel::clearBirthChartProfile,
                                    onOpenReading = viewModel::openReadingDetail,
                                    onDeleteReading = viewModel::deleteReading,
                                    onResetBaseline = viewModel::resetPersonalBaseline,
                                    onGoToScan = { navigateTo(NatureAuraRoute.SCAN) },
                                )
                            }

                            composable(NatureAuraRoute.THERAPY.route) {
                                TherapyScreen(
                                    activeBand = activeBand,
                                    secondaryBand = secondaryBand,
                                    isPlaying = isAudioPlaying,
                                    volume = audioVolume,
                                    elapsedSeconds = audioElapsedSeconds,
                                    sleepTimerOption = sleepTimerOption,
                                    breathPaceMode = uiState.breathPaceMode,
                                    lastScan = uiState.lastCompletedScan ?: uiState.liveAura,
                                    latestSavedId = savedReadings.firstOrNull()?.id,
                                    onSelectBand = viewModel::selectAndPlayBand,
                                    onTogglePlayPause = viewModel::toggleAudioPlayPause,
                                    onVolumeChange = viewModel::setAudioVolume,
                                    onSelectSleepTimer = viewModel::setSleepTimer,
                                    onSelectBreathPace = viewModel::setBreathPaceMode,
                                    onStartDeltaFollowUp = { parentId ->
                                        viewModel.startPostTherapyFollowUp(parentId)
                                        navigateTo(NatureAuraRoute.SCAN)
                                    },
                                )
                            }

                            composable(NatureAuraRoute.WEARABLES.route) {
                                WearablesScreen(
                                    sourceMode = sourceMode,
                                    wearOsAvailable = wearOsAvailable,
                                    connectedWearNodes = connectedWearNodes,
                                    discoveredBleDevices = discoveredBleDevices,
                                    isBleScanning = isBleScanning,
                                    statusMessage = connectionStatus,
                                    activePreset = activePreset,
                                    sensorCapabilities = sensorCapabilities,
                                    activeSensorSummary = activeSensorSummary,
                                    latestPayload = uiState.latestPayload,
                                    onEnableOnDeviceSensors = {
                                        viewModel.enableOnDeviceSensorMode()
                                        navigateTo(NatureAuraRoute.SCAN)
                                    },
                                    onRefreshWearNodes = viewModel::refreshWearNodes,
                                    onStartBleScan = viewModel::startBleScan,
                                    onStopBleScan = viewModel::stopBleScan,
                                    onConnectBleDevice = viewModel::connectBleDevice,
                                    onToggleCameraPpg = { enable ->
                                        viewModel.toggleCameraPpgMode(enable)
                                        if (enable) navigateTo(NatureAuraRoute.SCAN)
                                    },
                                    onSelectCalibrationPreset = viewModel::startCalibrationPreset,
                                    onStopCalibration = viewModel::stopCalibrationSignal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
