package com.example.natureaura.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.natureaura.engine.AuraColorSlice
import com.example.natureaura.engine.AuraReadingInterpreter
import com.example.natureaura.engine.AuraReadingRecord
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.BirthChartProfile
import com.example.natureaura.model.ChakraScore
import com.example.natureaura.model.ChakraType
import com.example.natureaura.ui.components.LocalAuraPhotoImage
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.CoralAlert
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun AuraReadingDetailScreen(
    record: AuraReadingRecord,
    parentRecord: AuraReadingRecord?,
    birthChartProfile: BirthChartProfile?,
    onSaveBirthChartProfile: (Int, Int, Int) -> Unit,
    onClearBirthChartProfile: () -> Unit,
    onBack: () -> Unit,
    onShare: (AuraReadingRecord) -> Unit,
    onDelete: (AuraReadingRecord) -> Unit,
    onLaunchTherapy: (AuraResult) -> Unit,
    onStartDeltaFollowUp: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val formattedDate = remember(record.timestampMs) {
        SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
            .format(Date(record.timestampMs))
    }

    val result = record.result
    val dominant = result.dominantChakra
    val sortedChakras = remember(result) { result.chakras.sortedByDescending { it.intensity } }
    val secondaryColor = sortedChakras.getOrElse(1) { dominant }.chakra.color
    val weakestColor = result.weakestChakra.chakra.color
    val colorSignature = remember(result) { AuraReadingInterpreter.computeColorSignature(result) }
    val enrichedNarrative = remember(record, birthChartProfile) {
        AuraReadingInterpreter.interpret(result, birthChartProfile)
    }
    var selectedNode by remember(record.id) { mutableStateOf<ChakraScore?>(dominant) }
    var showBirthChartDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SanctuaryForest, SanctuaryVoid)))
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reading_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = IvoryPrimary,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Aura Portrait & Reading",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryPrimary,
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = SageMuted,
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { onShare(record) },
                            modifier = Modifier.testTag("reading_share_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share aura photograph",
                                tint = CeladonAccent,
                            )
                        }
                        IconButton(
                            onClick = { onDelete(record) },
                            modifier = Modifier.testTag("reading_delete_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete reading",
                                tint = CoralAlert,
                            )
                        }
                    }
                }
            }

            // Before vs. After Delta Comparison Card (if this is a follow-up scan)
            if (parentRecord != null) {
                item {
                    val delta = remember(parentRecord, record) {
                        AuraReadingInterpreter.computeDelta(parentRecord, record)
                    }
                    Surface(
                        color = CeladonAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.55f)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "BEFORE VS. AFTER THERAPY SHIFT",
                                style = MaterialTheme.typography.labelSmall,
                                color = CeladonAccent,
                            )
                            Text(
                                text = delta.summary,
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryPrimary,
                            )
                        }
                    }
                }
            }

            // 1. Captured Aura Photograph Frame (Full un-cropped Polaroid with dual-hue aura glow)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, dominant.chakra.color.copy(alpha = 0.85f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        dominant.chakra.color.copy(alpha = 0.24f),
                                        secondaryColor.copy(alpha = 0.14f),
                                        SanctuarySurface,
                                    ),
                                ),
                            )
                            .padding(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(972f / 1440f)
                                .clip(RoundedCornerShape(18.dp)),
                        ) {
                            LocalAuraPhotoImage(
                                photoPath = record.photoPath,
                                contentDescription = "Captured Aura Photograph",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )

                            Surface(
                                color = SanctuaryVoid.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, dominant.chakra.color.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = "${(result.overallBalance * 100).toInt()}% HARMONY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dominant.chakra.color,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 1b. Aurla-Style Proportional Aura Color Signature & Meaning Breakdown
            item {
                AuraColorSignatureCard(
                    slices = colorSignature,
                    dominantAccent = dominant.chakra.color,
                )
            }

            // 2. Interactive Concentric Aura Layer Visualization + Written Archetype Reading
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, SanctuaryBorder),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = enrichedNarrative.energeticEssenceTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor,
                        )
                        Text(
                            text = enrichedNarrative.archetypeTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = dominant.chakra.color,
                        )
                        Text(
                            text = enrichedNarrative.readingNarrative,
                            style = MaterialTheme.typography.bodyLarge,
                            color = IvoryPrimary,
                        )

                        InteractiveConcentricAuraCanvas(
                            result = result,
                            selectedChakra = selectedNode?.chakra,
                            onSelectChakra = { selectedNode = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(256.dp),
                        )

                        selectedNode?.let { node ->
                            Surface(
                                color = node.chakra.color.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, node.chakra.color.copy(alpha = 0.5f)),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(
                                        text = "${node.chakra.englishName} (${node.chakra.sanskritName}) · ${(node.intensity * 100).toInt()}% · ${node.metricSnapshot}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = node.chakra.color,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = node.chakra.healingGuidance,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IvoryPrimary,
                                    )
                                }
                            }
                        }

                        AuraLayerCard(
                            title = "INNER CORE LAYER",
                            body = enrichedNarrative.innerLayerSummary,
                            accent = result.chakras.first { it.chakra == ChakraType.ROOT }.chakra.color,
                        )
                        AuraLayerCard(
                            title = "EMOTIONAL FIELD LAYER",
                            body = enrichedNarrative.emotionalLayerSummary,
                            accent = result.chakras.first { it.chakra == ChakraType.HEART }.chakra.color,
                        )
                        AuraLayerCard(
                            title = "OUTER CROWN LAYER",
                            body = enrichedNarrative.outerLayerSummary,
                            accent = result.chakras.first { it.chakra == ChakraType.CROWN }.chakra.color,
                        )
                    }
                }
            }

            // 2b. Aurla-Matched Ascendant & Descendant Polarity Axis + Optional Zodiac Tuning
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, dominant.chakra.color.copy(alpha = 0.55f)),
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
                                    text = "ASCENDANT & DESCENDANT AXIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CeladonAccent,
                                )
                                Text(
                                    text = if (birthChartProfile != null) {
                                        "Synced with Birth Chart (${birthChartProfile.birthMonth}/${birthChartProfile.birthDay} · ${birthChartProfile.birthHour24}:00)"
                                    } else {
                                        "Auto-computed from biofield polarity"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SageMuted,
                                )
                            }
                            OutlinedButton(
                                onClick = { showBirthChartDialog = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tune_birth_chart_button"),
                                border = BorderStroke(1.dp, CeladonAccent.copy(alpha = 0.65f)),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = CeladonAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (birthChartProfile != null) "Edit Chart" else "Birth Chart",
                                    color = CeladonAccent,
                                )
                            }
                        }

                        PolarityAxisCard(
                            label = "ASCENDANT · OUTWARD PROJECTION",
                            headline = enrichedNarrative.ascendantTitle,
                            body = enrichedNarrative.ascendantSummary,
                            accent = dominant.chakra.color,
                            icon = Icons.Default.NorthEast,
                        )

                        PolarityAxisCard(
                            label = "DESCENDANT · RELATIONAL MAGNET",
                            headline = enrichedNarrative.descendantTitle,
                            body = enrichedNarrative.descendantSummary,
                            accent = weakestColor,
                            icon = Icons.Default.SouthWest,
                        )
                    }
                }
            }

            // 2c. Aurla-Matched Personality, Love & Career Insights + Daily Mantra
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, SanctuaryBorder),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "DEEP AURA INSIGHTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmber,
                        )

                        InsightDetailRow(
                            title = "PERSONALITY & STRENGTHS",
                            body = enrichedNarrative.personalityInsight,
                            accent = dominant.chakra.color,
                            icon = Icons.Default.Psychology,
                        )

                        InsightDetailRow(
                            title = "LOVE & COMPATIBILITY",
                            body = enrichedNarrative.loveInsight,
                            accent = result.chakras.first { it.chakra == ChakraType.HEART }.chakra.color,
                            icon = Icons.Default.Favorite,
                        )

                        InsightDetailRow(
                            title = "CAREER & FOCUS WINDOW",
                            body = enrichedNarrative.careerInsight,
                            accent = result.chakras.first { it.chakra == ChakraType.SOLAR_PLEXUS }.chakra.color,
                            icon = Icons.Default.WorkOutline,
                        )

                        Surface(
                            color = dominant.chakra.color.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, dominant.chakra.color.copy(alpha = 0.55f)),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = dominant.chakra.color,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "DAILY AURA MANTRA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dominant.chakra.color,
                                    )
                                }
                                Text(
                                    text = enrichedNarrative.dailyMantra,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // 3. Biometric Snapshot + Sound Therapy + Post-Therapy Delta Scan
            item {
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
                            text = "OFFLINE TELEMETRY SNAPSHOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CeladonAccent,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TelemetryMiniStat("HR", "${result.hrMeanBpm.toInt()} bpm")
                            TelemetryMiniStat("HRV", "${result.hrvMeanMs.toInt()} ms")
                            TelemetryMiniStat("TEMP", String.format(Locale.US, "%.1f°C", result.skinTempMeanC))
                            TelemetryMiniStat("EDA", String.format(Locale.US, "%.1fµS", result.edaMeanMicroS))
                        }

                        Spacer(Modifier.height(4.dp))

                        result.chakras.reversed().forEach { score ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${score.chakra.englishName} (${score.statusLabel})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IvoryPrimary,
                                    )
                                    Text(
                                        text = "${(score.intensity * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = score.chakra.color,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { score.intensity },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = score.chakra.color,
                                    trackColor = SanctuaryVoid,
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { onLaunchTherapy(result) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("detail_launch_therapy_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = result.prescription.primaryBand.accentColor,
                                contentColor = SanctuaryVoid,
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play ${result.prescription.primaryBand.title} (${result.prescription.primaryBand.frequencyLabel})")
                        }

                        OutlinedButton(
                            onClick = { onStartDeltaFollowUp(record.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_delta_followup_button"),
                            border = BorderStroke(1.dp, CeladonAccent),
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = CeladonAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Run Post-Therapy Follow-Up Scan", color = CeladonAccent)
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
private fun AuraColorSignatureCard(
    slices: List<AuraColorSlice>,
    dominantAccent: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanctuarySurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, dominantAccent.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(dominantAccent.copy(alpha = 0.14f), SanctuarySurface),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AURA COLOR SIGNATURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = dominantAccent,
                )
                Text(
                    text = "${slices.firstOrNull()?.colorName ?: ""} Lead",
                    style = MaterialTheme.typography.labelSmall,
                    color = IvoryPrimary,
                )
            }

            // Proportional full-spectrum color bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
            ) {
                slices.forEach { slice ->
                    Box(
                        modifier = Modifier
                            .weight(slice.percentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(slice.color),
                    )
                }
            }

            // Top 4 dominant hue cards with percentage & color meaning
            slices.take(4).forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(slice.color),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${slice.colorName} · ${slice.chakra.englishName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = IvoryPrimary,
                        )
                        Text(
                            text = slice.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = SageMuted,
                        )
                    }
                    Surface(
                        color = slice.color.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, slice.color.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = "${slice.percentage}%",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = slice.color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveConcentricAuraCanvas(
    result: AuraResult,
    selectedChakra: ChakraType?,
    onSelectChakra: (ChakraScore) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(result) { result.chakras.sortedByDescending { it.intensity } }
    val c1 = sorted[0].chakra.color
    val c2 = sorted.getOrElse(1) { sorted[0] }.chakra.color
    val c3 = sorted.getOrElse(2) { sorted[0] }.chakra.color

    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(result) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = minOf(size.width, size.height) * 0.44f
                    var bestScore: ChakraScore? = null
                    var bestDist = Float.MAX_VALUE
                    result.chakras.forEachIndexed { idx, score ->
                        val angle = (-PI / 2.0) + (idx * 2.0 * PI / 7.0)
                        val orbitR = maxR * (0.45f + score.intensity * 0.48f)
                        val pt = Offset(
                            x = center.x + (cos(angle) * orbitR).toFloat(),
                            y = center.y + (sin(angle) * orbitR).toFloat(),
                        )
                        val d = hypot(tapOffset.x - pt.x, tapOffset.y - pt.y)
                        if (d < bestDist && d < 64f) {
                            bestDist = d
                            bestScore = score
                        }
                    }
                    bestScore?.let(onSelectChakra)
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.45f

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to c1.copy(alpha = 0.85f),
                    0.40f to c2.copy(alpha = 0.58f),
                    0.76f to c3.copy(alpha = 0.34f),
                    1.0f to Color.Transparent,
                ),
                center = center,
                radius = maxR * 1.24f,
            ),
            radius = maxR * 1.24f,
            center = center,
        )

        // Secondary off-center additive lobe for rich Kirlian plasma depth
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to c2.copy(alpha = 0.52f),
                    0.65f to c3.copy(alpha = 0.25f),
                    1.0f to Color.Transparent,
                ),
                center = Offset(center.x - maxR * 0.25f, center.y - maxR * 0.18f),
                radius = maxR * 0.92f,
            ),
            radius = maxR * 0.92f,
            center = Offset(center.x - maxR * 0.25f, center.y - maxR * 0.18f),
            blendMode = BlendMode.Plus,
        )

        drawCircle(color = c1.copy(alpha = 0.92f), radius = maxR * 0.36f, center = center, style = Stroke(2.5.dp.toPx()))
        drawCircle(color = c2.copy(alpha = 0.82f), radius = maxR * 0.64f, center = center, style = Stroke(2.5.dp.toPx()))
        drawCircle(color = c3.copy(alpha = 0.72f), radius = maxR * 0.92f, center = center, style = Stroke(2.dp.toPx()))

        result.chakras.forEachIndexed { idx, score ->
            val angle = (-PI / 2.0) + (idx * 2.0 * PI / 7.0)
            val orbitR = maxR * (0.45f + score.intensity * 0.48f)
            val pt = Offset(
                x = center.x + (cos(angle) * orbitR).toFloat(),
                y = center.y + (sin(angle) * orbitR).toFloat(),
            )
            val nodeRadius = 6.dp.toPx() + score.intensity * 5.dp.toPx()

            // Outer chromatic glow halo around every chakra node
            drawCircle(
                color = score.chakra.color.copy(alpha = 0.34f),
                radius = nodeRadius * 2.1f,
                center = pt,
                blendMode = BlendMode.Plus,
            )

            if (score.chakra == selectedChakra) {
                drawCircle(
                    color = IvoryPrimary,
                    radius = nodeRadius + 4.dp.toPx(),
                    center = pt,
                    style = Stroke(2.dp.toPx()),
                )
            }
            drawCircle(
                color = score.chakra.color,
                radius = nodeRadius,
                center = pt,
            )
        }
    }
}

@Composable
private fun AuraLayerCard(
    title: String,
    body: String,
    accent: Color,
) {
    Surface(
        color = SanctuaryVoid,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.60f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.18f), SanctuaryVoid),
                    ),
                )
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryPrimary,
            )
        }
    }
}

@Composable
private fun PolarityAxisCard(
    label: String,
    headline: String,
    body: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        color = SanctuaryVoid,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.20f), SanctuaryVoid),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = IvoryPrimary,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = SageMuted,
            )
        }
    }
}

@Composable
private fun InsightDetailRow(
    title: String,
    body: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        color = SanctuaryVoid,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryPrimary,
            )
        }
    }
}

@Composable
internal fun BirthChartTuningDialog(
    initialProfile: BirthChartProfile?,
    onSave: (Int, Int, Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var month by remember(initialProfile) { mutableIntStateOf(initialProfile?.birthMonth ?: 6) }
    var day by remember(initialProfile) { mutableIntStateOf(initialProfile?.birthDay ?: 15) }
    var hour by remember(initialProfile) { mutableIntStateOf(initialProfile?.birthHour24 ?: 9) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SanctuarySurface,
        titleContentColor = IvoryPrimary,
        textContentColor = SageMuted,
        title = { Text("Offline Zodiac Ascendant Tuning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Optional offline birth month, day & hour enriches your energetic Ascendant & Descendant with your astronomical Sun, Rising & 7th-House Descendant signs. Stored locally only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SageMuted,
                )
                StepperRow("Birth Month", month, 1..12) { month = it }
                StepperRow("Birth Day", day, 1..31) { day = it }
                StepperRow("Birth Hour (0-23)", hour, 0..23) { hour = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(month, day, hour) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CeladonAccent,
                    contentColor = SanctuaryVoid,
                ),
            ) {
                Text("Save Offline")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (initialProfile != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = CoralAlert)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = IvoryPrimary)
                }
            }
        },
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = IvoryPrimary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    onValueChange(if (value <= range.first) range.last else value - 1)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, SanctuaryBorder),
            ) {
                Text("-", color = IvoryPrimary)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleMedium,
                color = CeladonAccent,
            )
            OutlinedButton(
                onClick = {
                    onValueChange(if (value >= range.last) range.first else value + 1)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, SanctuaryBorder),
            ) {
                Text("+", color = IvoryPrimary)
            }
        }
    }
}

@Composable
private fun TelemetryMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SageMuted)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.titleMedium,
            color = WarmAmber,
        )
    }
}
