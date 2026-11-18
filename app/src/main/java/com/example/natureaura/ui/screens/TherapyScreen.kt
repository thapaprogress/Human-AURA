package com.example.natureaura.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.BreathPaceMode
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.SleepTimerOption
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryBorder
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuarySurface
import com.example.ui.theme.SanctuaryVoid
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun TherapyScreen(
    activeBand: FrequencyBand,
    secondaryBand: FrequencyBand?,
    isPlaying: Boolean,
    volume: Float,
    elapsedSeconds: Int,
    sleepTimerOption: SleepTimerOption,
    breathPaceMode: BreathPaceMode,
    lastScan: AuraResult?,
    latestSavedId: Long?,
    onSelectBand: (FrequencyBand) -> Unit,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onSelectBreathPace: (BreathPaceMode) -> Unit,
    onStartDeltaFollowUp: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    fun handlePlayToggle() {
        if (!isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onTogglePlayPause()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SanctuaryVoid),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_aura_sanctuary_bg),
            contentDescription = "Healing sound sanctuary backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SanctuaryVoid.copy(alpha = 0.72f),
                            SanctuaryVoid.copy(alpha = 0.90f),
                        ),
                    ),
                ),
        )

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "Frequency Sound Therapy",
                    style = MaterialTheme.typography.headlineLarge,
                    color = IvoryPrimary,
                )
                Text(
                    text = "Offline mediaPlayback service · Alpha, Beta, Gamma & Aum",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SageMuted,
                )
            }

            if (lastScan != null) {
                item {
                    Surface(
                        color = lastScan.prescription.primaryBand.accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            lastScan.prescription.primaryBand.accentColor.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "SCAN ROUTING · WEAKEST: ${lastScan.weakestChakra.chakra.englishName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = lastScan.prescription.primaryBand.accentColor,
                            )
                            Text(
                                text = lastScan.prescription.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvoryPrimary,
                            )
                            if (latestSavedId != null) {
                                OutlinedButton(
                                    onClick = { onStartDeltaFollowUp(latestSavedId) },
                                    modifier = Modifier.height(48.dp),
                                    border = BorderStroke(1.dp, CeladonAccent),
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = CeladonAccent)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Run Post-Therapy Follow-Up Scan", color = CeladonAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Live Harmonic Waveform + Sleep Timer Player Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface.copy(alpha = 0.90f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, activeBand.accentColor.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (secondaryBand != null) {
                                        "${activeBand.title} + ${secondaryBand.title}"
                                    } else {
                                        activeBand.title
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = IvoryPrimary,
                                )
                                val timerInfo = sleepTimerOption.durationSeconds?.let { limit ->
                                    val rem = (limit - elapsedSeconds).coerceAtLeast(0)
                                    " · Sleep in ${formatDuration(rem)}"
                                } ?: ""
                                Text(
                                    text = "${activeBand.frequencyLabel} · ${formatDuration(elapsedSeconds)}$timerInfo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeBand.accentColor,
                                )
                            }

                            FilledIconButton(
                                onClick = ::handlePlayToggle,
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("therapy_play_pause_button"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = activeBand.accentColor,
                                    contentColor = SanctuaryVoid,
                                ),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        HarmonicWaveformCanvas(
                            band = activeBand,
                            isPlaying = isPlaying,
                            amplitudeScale = volume,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = SageMuted,
                            )
                            Slider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("therapy_volume_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = activeBand.accentColor,
                                    activeTrackColor = activeBand.accentColor,
                                ),
                            )
                        }

                        // Sleep Timer Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Sleep Timer",
                                tint = SageMuted,
                                modifier = Modifier.size(18.dp),
                            )
                            SleepTimerOption.entries.forEach { opt ->
                                val selected = sleepTimerOption == opt
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectSleepTimer(opt) },
                                    label = { Text(opt.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = SanctuaryVoid,
                                        labelColor = IvoryPrimary,
                                        selectedContainerColor = activeBand.accentColor.copy(alpha = 0.22f),
                                        selectedLabelColor = activeBand.accentColor,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = SanctuaryBorder,
                                        selectedBorderColor = activeBand.accentColor,
                                    ),
                                )
                            }
                        }

                        if (activeBand.isBinaural) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = SageMuted,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Stereo headphones recommended for ${activeBand.beatHz.toInt()} Hz binaural beat (${activeBand.carrierHz.toInt()} Hz L / ${(activeBand.carrierHz + activeBand.beatHz).toInt()} Hz R)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SageMuted,
                                )
                            }
                        }
                    }
                }
            }

            // Guided Breath Pacer Card
            item {
                GuidedBreathPacerCard(
                    mode = breathPaceMode,
                    accentColor = activeBand.accentColor,
                    isActive = isPlaying,
                    onSelectMode = onSelectBreathPace,
                )
            }

            items(FrequencyBand.entries, key = { it.name }) { band ->
                val selected = band == activeBand
                Surface(
                    color = if (selected) band.accentColor.copy(alpha = 0.14f) else SanctuaryForest.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) band.accentColor else SanctuaryBorder,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectBand(band) }
                        .testTag("band_card_${band.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(band.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = band.frequencyLabel.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = band.accentColor,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${band.title} (${band.frequencyLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryPrimary,
                            )
                            Text(
                                text = band.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SageMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedBreathPacerCard(
    mode: BreathPaceMode,
    accentColor: Color,
    isActive: Boolean,
    onSelectMode: (BreathPaceMode) -> Unit,
) {
    val totalCycleSec = mode.inhaleSec + mode.holdSec + mode.exhaleSec + mode.holdAfterSec
    var cycleTime by remember(mode) { mutableFloatStateOf(0f) }

    LaunchedEffect(mode, isActive) {
        while (true) {
            delay(50L)
            cycleTime = (cycleTime + 0.05f) % totalCycleSec
        }
    }

    val (phaseLabel, expansion) = when {
        cycleTime < mode.inhaleSec -> {
            val p = (cycleTime / mode.inhaleSec).coerceIn(0f, 1f)
            "Inhale Slowly" to (0.35f + 0.65f * p)
        }
        cycleTime < mode.inhaleSec + mode.holdSec -> {
            "Hold Breath" to 1.0f
        }
        cycleTime < mode.inhaleSec + mode.holdSec + mode.exhaleSec -> {
            val t = cycleTime - mode.inhaleSec - mode.holdSec
            val p = (t / mode.exhaleSec).coerceIn(0f, 1f)
            "Exhale Softly" to (1.0f - 0.65f * p)
        }
        else -> {
            "Rest Empty" to 0.35f
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SanctuaryBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "GUIDED BREATH PACER",
                style = MaterialTheme.typography.labelSmall,
                color = CeladonAccent,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = (size.minDimension / 2f) * expansion
                        drawCircle(
                            color = accentColor.copy(alpha = 0.25f),
                            radius = r,
                        )
                        drawCircle(
                            color = accentColor,
                            radius = r,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phaseLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary,
                    )
                    Text(
                        text = "${mode.title} · Synced to cardiac HRV resonance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageMuted,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BreathPaceMode.entries.forEach { item ->
                    val selected = item == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMode(item) },
                        label = { Text(item.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SanctuaryVoid,
                            labelColor = IvoryPrimary,
                            selectedContainerColor = CeladonAccent.copy(alpha = 0.22f),
                            selectedLabelColor = CeladonAccent,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = SanctuaryBorder,
                            selectedBorderColor = CeladonAccent,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmonicWaveformCanvas(
    band: FrequencyBand,
    isPlaying: Boolean,
    amplitudeScale: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )

    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        val cycles = when (band) {
            FrequencyBand.AUM -> 2.5f
            FrequencyBand.ALPHA -> 4.0f
            FrequencyBand.BETA -> 6.5f
            FrequencyBand.GAMMA -> 10.0f
        }
        val amp = if (isPlaying) size.height * 0.36f * amplitudeScale.coerceIn(0.25f, 1f) else size.height * 0.08f
        val activePhase = if (isPlaying) phase else 0f

        val primaryPath = Path()
        val harmonicPath = Path()

        val steps = 120
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = t * size.width
            val envelope = sin(t * PI).toFloat()
            val y1 = midY + sin(t * cycles * 2f * PI.toFloat() + activePhase) * amp * envelope
            val y2 = midY + sin(t * cycles * 4f * PI.toFloat() - activePhase * 1.4f) * (amp * 0.45f) * envelope
            if (i == 0) {
                primaryPath.moveTo(x, y1)
                harmonicPath.moveTo(x, y2)
            } else {
                primaryPath.lineTo(x, y1)
                harmonicPath.lineTo(x, y2)
            }
        }

        drawPath(
            path = harmonicPath,
            color = Color.White.copy(alpha = 0.22f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawPath(
            path = primaryPath,
            color = band.accentColor,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
