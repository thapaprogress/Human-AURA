package com.example.natureaura.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.ChakraScore
import com.example.ui.theme.CeladonAccent
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.SageMuted
import com.example.ui.theme.SanctuaryBorder
import com.example.ui.theme.SanctuaryForest
import com.example.ui.theme.SanctuarySurface
import com.example.ui.theme.SanctuaryVoid
import com.example.ui.theme.WarmAmber
import java.util.Locale

@Composable
fun ChakraBreakdownScreen(
    auraResult: AuraResult?,
    onStartScanTab: () -> Unit,
    onLaunchTherapy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SanctuaryForest, SanctuaryVoid),
                ),
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (auraResult == null) {
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
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CeladonAccent,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "7-Chakra Biometric Spectrum",
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvoryPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Run a 15-second Aura Scan or activate a wearable stream to map Heart Rate variance, HRV coherence, Skin Temperature, and EDA across all seven chakras.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SageMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStartScanTab,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CeladonAccent,
                        contentColor = SanctuaryVoid,
                    ),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Go to Aura Scan")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Chakra Biofield Map",
                        style = MaterialTheme.typography.headlineLarge,
                        color = IvoryPrimary,
                    )
                    Text(
                        text = "Mean HR ${auraResult.hrMeanBpm.toInt()} bpm · HRV ${auraResult.hrvMeanMs.toInt()} ms · Temp ${String.format(Locale.US, "%.1f", auraResult.skinTempMeanC)}°C · EDA ${String.format(Locale.US, "%.1f", auraResult.edaMeanMicroS)} µS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SageMuted,
                    )
                }

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
                            ) {
                                HighlightBadge(
                                    title = "DOMINANT CENTER",
                                    name = auraResult.dominantChakra.chakra.englishName,
                                    percent = (auraResult.dominantChakra.intensity * 100).toInt(),
                                    color = auraResult.dominantChakra.chakra.color,
                                )
                                HighlightBadge(
                                    title = "WEAKEST CENTER",
                                    name = auraResult.weakestChakra.chakra.englishName,
                                    percent = (auraResult.weakestChakra.intensity * 100).toInt(),
                                    color = auraResult.weakestChakra.chakra.color,
                                )
                            }

                            Text(
                                text = auraResult.prescription.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SageMuted,
                            )

                            Button(
                                onClick = onLaunchTherapy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("chakra_rebalance_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = auraResult.prescription.primaryBand.accentColor,
                                    contentColor = SanctuaryVoid,
                                ),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Rebalance via ${auraResult.prescription.primaryBand.title} (${auraResult.prescription.primaryBand.frequencyLabel})")
                            }
                        }
                    }
                }

                // Render Crown -> Root anatomical top-to-bottom order
                items(auraResult.chakras.reversed(), key = { it.chakra.name }) { score ->
                    ChakraRowCard(
                        score = score,
                        isWeakest = score.chakra == auraResult.weakestChakra.chakra,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightBadge(
    title: String,
    name: String,
    percent: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = SageMuted,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = "$name ($percent%)",
                style = MaterialTheme.typography.titleMedium,
                color = IvoryPrimary,
            )
        }
    }
}

@Composable
private fun ChakraRowCard(
    score: ChakraScore,
    isWeakest: Boolean,
) {
    val chakra = score.chakra
    val borderColor = if (isWeakest) WarmAmber.copy(alpha = 0.7f) else SanctuaryBorder

    Surface(
        color = SanctuarySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isWeakest) WarmAmber.copy(alpha = 0.85f) else chakra.color.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(chakra.color.copy(alpha = 0.15f), SanctuarySurface),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(chakra.color),
                    )
                    Column {
                        Text(
                            text = "${chakra.englishName} · ${chakra.sanskritName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryPrimary,
                        )
                        Text(
                            text = chakra.primaryDriver,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SageMuted,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(score.intensity * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = chakra.color,
                    )
                    Text(
                        text = score.metricSnapshot,
                        style = MaterialTheme.typography.labelSmall,
                        color = SageMuted,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { score.intensity },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = chakra.color,
                trackColor = SanctuaryVoid,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = score.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isWeakest) WarmAmber else chakra.color,
                )
                if (isWeakest) {
                    Text(
                        text = "TARGETED FOR SOUND THERAPY",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                    )
                }
            }
        }
    }
}
