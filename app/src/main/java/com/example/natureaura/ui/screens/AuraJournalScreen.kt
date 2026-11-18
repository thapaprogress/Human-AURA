package com.example.natureaura.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.natureaura.engine.AuraReadingRecord
import com.example.natureaura.model.BirthChartProfile
import com.example.natureaura.model.PersonalBaseline
import com.example.natureaura.ui.components.LocalAuraPhotoImage
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryBorder
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuarySurface
import com.example.ui.theme.SanctuaryVoid
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class JournalTabMode(val label: String) {
    GALLERY("Photo Gallery"),
    TRENDS("Trends & Baseline"),
}

@Composable
fun AuraJournalScreen(
    readings: List<AuraReadingRecord>,
    baseline: PersonalBaseline?,
    birthChartProfile: BirthChartProfile?,
    onSaveBirthChartProfile: (Int, Int, Int) -> Unit,
    onClearBirthChartProfile: () -> Unit,
    onOpenReading: (AuraReadingRecord) -> Unit,
    onDeleteReading: (AuraReadingRecord) -> Unit,
    onResetBaseline: () -> Unit,
    onGoToScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tabMode by remember { mutableStateOf(JournalTabMode.GALLERY) }
    var showBirthChartDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SanctuaryForest, SanctuaryVoid)))
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (readings.isEmpty() && baseline == null) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CeladonAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = CeladonAccent,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Offline Auric Photo Journal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvoryPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Every captured Aura Photograph and 7-Chakra reading is stored locally on your device in Room SQLite—100% offline and private.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SageMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGoToScan,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CeladonAccent,
                        contentColor = SanctuaryVoid,
                    ),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Capture Aura Photograph")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Auric Photo Journal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = IvoryPrimary,
                )
                Text(
                    text = "${readings.size} offline reading${if (readings.size == 1) "" else "s"} · Room SQLite + Local Portrait Storage",
                    style = MaterialTheme.typography.labelSmall,
                    color = CeladonAccent,
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JournalTabMode.entries.forEach { mode ->
                        val selected = tabMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { tabMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SanctuarySurface,
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

                Spacer(Modifier.height(10.dp))

                if (tabMode == JournalTabMode.GALLERY) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 168.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(readings, key = { it.id }) { record ->
                            JournalPhotoCard(
                                record = record,
                                onClick = { onOpenReading(record) },
                                onDelete = { onDeleteReading(record) },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            HarmonyTrendChartCard(readings = readings)
                        }
                        item {
                            PersonalBaselineCard(
                                baseline = baseline,
                                onReset = onResetBaseline,
                            )
                        }
                        item {
                            BirthChartProfileCard(
                                profile = birthChartProfile,
                                onConfigure = { showBirthChartDialog = true },
                                onClear = onClearBirthChartProfile,
                            )
                        }
                    }
                }
            }
        }

        if (showBirthChartDialog) {
            BirthChartTuningDialog(
                initialProfile = birthChartProfile,
                onSave = { m, d, h ->
                    onSaveBirthChartProfile(m, d, h)
                    showBirthChartDialog = false
                },
                onClear = {
                    onClearBirthChartProfile()
                    showBirthChartDialog = false
                },
                onDismiss = { showBirthChartDialog = false },
            )
        }
    }
}

@Composable
private fun HarmonyTrendChartCard(readings: List<AuraReadingRecord>) {
    val chronological = remember(readings) { readings.take(14).reversed() }

    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SanctuaryBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "LONGITUDINAL HARMONY & HRV TRENDS",
                style = MaterialTheme.typography.labelSmall,
                color = CeladonAccent,
            )

            if (chronological.size < 2) {
                Text(
                    text = "Complete at least 2 Aura Scans to plot your longitudinal Harmony % and HRV coherence curve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SageMuted,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val pad = 16.dp.toPx()
                    val usableW = (w - pad * 2f).coerceAtLeast(1f)
                    val usableH = (h - pad * 2f).coerceAtLeast(1f)

                    // Reference gridlines
                    listOf(0.25f, 0.5f, 0.75f).forEach { level ->
                        val y = pad + usableH * (1f - level)
                        drawLine(
                            color = SanctuaryBorder.copy(alpha = 0.6f),
                            start = Offset(pad, y),
                            end = Offset(w - pad, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val harmonyPath = Path()
                    val hrvPath = Path()

                    chronological.forEachIndexed { idx, rec ->
                        val x = pad + usableW * (idx.toFloat() / (chronological.size - 1).toFloat())
                        val yHarmony = pad + usableH * (1f - rec.result.overallBalance.coerceIn(0f, 1f))
                        val hrvNorm = ((rec.result.hrvMeanMs - 15.0) / 95.0).toFloat().coerceIn(0f, 1f)
                        val yHrv = pad + usableH * (1f - hrvNorm)

                        if (idx == 0) {
                            harmonyPath.moveTo(x, yHarmony)
                            hrvPath.moveTo(x, yHrv)
                        } else {
                            harmonyPath.lineTo(x, yHarmony)
                            hrvPath.lineTo(x, yHrv)
                        }
                    }

                    drawPath(
                        path = hrvPath,
                        color = WarmAmber.copy(alpha = 0.75f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawPath(
                        path = harmonyPath,
                        color = CeladonAccent,
                        style = Stroke(width = 3.dp.toPx()),
                    )

                    chronological.forEachIndexed { idx, rec ->
                        val x = pad + usableW * (idx.toFloat() / (chronological.size - 1).toFloat())
                        val yHarmony = pad + usableH * (1f - rec.result.overallBalance.coerceIn(0f, 1f))
                        drawCircle(
                            color = rec.result.dominantChakra.chakra.color,
                            radius = 5.dp.toPx(),
                            center = Offset(x, yHarmony),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "● Harmony % (Chakra Dot)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CeladonAccent,
                    )
                    Text(
                        text = "— HRV SDNN (ms)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalBaselineCard(
    baseline: PersonalBaseline?,
    onReset: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
        shape = RoundedCornerShape(22.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ADAPTIVE PERSONAL BASELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                    )
                    Text(
                        text = if (baseline != null && baseline.scanCount >= 2) {
                            "Calibrated across ${baseline.scanCount} offline scans"
                        } else {
                            "Needs ${2 - (baseline?.scanCount ?: 0)} more scan(s) to personalize thresholds"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageMuted,
                    )
                }
                if (baseline != null) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.height(48.dp),
                        border = BorderStroke(1.dp, SanctuaryBorder),
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = IvoryPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = IvoryPrimary)
                    }
                }
            }

            if (baseline != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BaselineStat("REST HR", "${baseline.restingHrBpm.toInt()} bpm")
                    BaselineStat("HRV", "${baseline.baselineHrvMs.toInt()} ms")
                    BaselineStat("TEMP", String.format(Locale.US, "%.1f°C", baseline.baselineSkinTempC))
                    BaselineStat("EDA", String.format(Locale.US, "%.1fµS", baseline.baselineEdaMicroS))
                }
            }
        }
    }
}

@Composable
private fun BirthChartProfileCard(
    profile: BirthChartProfile?,
    onConfigure: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "OPTIONAL ZODIAC ASCENDANT PROFILE",
                style = MaterialTheme.typography.labelSmall,
                color = CeladonAccent,
            )
            Text(
                text = if (profile != null) {
                    "Configured offline: Month ${profile.birthMonth}, Day ${profile.birthDay}, Hour ${profile.birthHour24}:00. All Aura Readings display synced Sun, Rising & Descendant polarity."
                } else {
                    "Add your birth date & hour offline to layer astronomical Rising & 7th-House Descendant signs onto every Aura Reading."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SageMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, CeladonAccent),
                ) {
                    Text(
                        text = if (profile != null) "Edit Birth Chart" else "Set Birth Chart",
                        color = CeladonAccent,
                    )
                }
                if (profile != null) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.height(48.dp),
                        border = BorderStroke(1.dp, SanctuaryBorder),
                    ) {
                        Text("Clear", color = IvoryPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BaselineStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SageMuted)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.titleMedium,
            color = IvoryPrimary,
        )
    }
}

@Composable
private fun JournalPhotoCard(
    record: AuraReadingRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val sorted = remember(record) { record.result.chakras.sortedByDescending { it.intensity } }
    val dominant = record.result.dominantChakra
    val formatted = remember(record.timestampMs) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(record.timestampMs))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, dominant.chakra.color.copy(alpha = 0.72f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("journal_card_${record.id}"),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            dominant.chakra.color.copy(alpha = 0.16f),
                            SanctuarySurface,
                        ),
                    ),
                )
                .padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(972f / 1440f)
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                LocalAuraPhotoImage(
                    photoPath = record.photoPath,
                    contentDescription = "Aura photograph ${record.narrative.archetypeTitle}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    color = SanctuaryVoid.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, dominant.chakra.color.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Text(
                        text = if (record.parentReadingId != null) {
                            "DELTA · ${(record.result.overallBalance * 100).toInt()}%"
                        } else {
                            "${(record.result.overallBalance * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = dominant.chakra.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete saved photo",
                        tint = IvoryPrimary.copy(alpha = 0.88f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            ) {
                sorted.take(4).forEach { score ->
                    Box(
                        modifier = Modifier
                            .weight(score.intensity.coerceAtLeast(0.1f))
                            .fillMaxSize()
                            .background(score.chakra.color),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = record.narrative.archetypeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = IvoryPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dominant.chakra.colorName,
                    style = MaterialTheme.typography.labelSmall,
                    color = dominant.chakra.color,
                )
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = SageMuted,
                )
            }
        }
    }
}
