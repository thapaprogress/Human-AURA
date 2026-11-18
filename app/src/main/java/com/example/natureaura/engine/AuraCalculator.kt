package com.example.natureaura.engine

import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.ChakraScore
import com.example.natureaura.model.ChakraType
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.PersonalBaseline
import com.example.natureaura.model.TherapyPrescription
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Biometric-to-Chakra Processing Engine.
 *
 * Includes:
 * 1. Motion/PPG artifact rejection (signalQuality < 0.45 and >35 bpm median spikes).
 * 2. Optional adaptive normalization against the user's rolling PersonalBaseline.
 * 3. 7-Chakra intensity mapping & Frequency Therapy prescription routing.
 */
class AuraCalculator {

    fun calculate(
        samples: List<BiometricPayload>,
        baseline: PersonalBaseline? = null,
    ): AuraResult {
        require(samples.isNotEmpty()) { "At least one biometric payload is required." }

        val qualityFiltered = samples.filter { it.signalQuality >= 0.45 }
            .ifEmpty { samples }

        val sortedHrs = qualityFiltered.map { it.heartRateBpm }.sorted()
        val medianHr = sortedHrs[sortedHrs.size / 2]

        val cleanSamples = if (qualityFiltered.size >= 4) {
            qualityFiltered.filter { abs(it.heartRateBpm - medianHr) <= 35.0 }
                .ifEmpty { qualityFiltered }
        } else {
            qualityFiltered
        }

        val filteredOutlierCount = (samples.size - cleanSamples.size).coerceAtLeast(0)
        val effectiveQuality = cleanSamples.map { it.signalQuality.coerceIn(0.0, 1.0) }.average()

        val hrValues = cleanSamples.map { it.heartRateBpm.coerceIn(38.0, 210.0) }
        val hrMean = hrValues.average()
        val hrStdDev = computeStdDev(hrValues, hrMean)

        val hrvMean = cleanSamples.map { it.hrvSdnnMs.coerceIn(5.0, 180.0) }.average()
        val tempMean = cleanSamples.map { it.skinTempCelsius.coerceIn(28.0, 40.0) }.average()
        val edaMean = cleanSamples.map { it.edaMicroSiemens.coerceIn(0.1, 30.0) }.average()
        val restingHr = cleanSamples.mapNotNull { it.restingHeartRateBpm }
            .takeIf { it.isNotEmpty() }?.average() ?: hrMean

        // Adaptive baseline adjustment when user has calibrated >= 2 scans
        val hasCalibratedBaseline = baseline != null && baseline.scanCount >= 2
        val targetRestHr = if (hasCalibratedBaseline) {
            (64.0 * 0.5 + baseline!!.restingHrBpm * 0.5)
        } else {
            64.0
        }
        val hrvBaselineBoost = if (hasCalibratedBaseline) {
            ((hrvMean - baseline!!.baselineHrvMs) / 60.0).coerceIn(-0.18, 0.18)
        } else {
            0.0
        }

        // 1. Root (Red): Baseline biometric stability (inverse of HR variance/stdDev + calm BPM)
        val stabilityFactor = (1.0 - (hrStdDev / 12.0)).coerceIn(0.0, 1.0)
        val bpmStrainPenalty = ((hrMean - (targetRestHr + 21.0)) / 45.0).coerceIn(0.0, 0.45)
        val rootIntensity = (stabilityFactor * 0.82 + 0.18 - bpmStrainPenalty).coerceIn(0.08, 1.0).toFloat()

        // 2. Sacral (Orange): EDA / skin moisture & sympathetic HRV arousal markers.
        val edaNorm = ((edaMean - 0.5) / 14.0).coerceIn(0.0, 1.0)
        val hrvStressFactor = (1.0 - ((hrvMean - 18.0) / 70.0)).coerceIn(0.0, 1.0)
        val sacralIntensity = (edaNorm * 0.65 + hrvStressFactor * 0.35).coerceIn(0.08, 1.0).toFloat()

        // 3. Solar Plexus (Yellow): Skin temperature + steady heart rate.
        val tempNorm = ((tempMean - 31.8) / 4.2).coerceIn(0.0, 1.0)
        val steadyHrFactor = (1.0 - (hrStdDev / 10.0)).coerceIn(0.0, 1.0)
        val solarIntensity = (tempNorm * 0.68 + steadyHrFactor * 0.32).coerceIn(0.08, 1.0).toFloat()

        // 4. Heart (Green): Peak Heart Rate Variability (HRV SDNN) & rhythm coherence.
        val hrvCoherence = (((hrvMean - 15.0) / 85.0) + hrvBaselineBoost).coerceIn(0.0, 1.0)
        val heartIntensity = (hrvCoherence * 0.78 + stabilityFactor * 0.22).coerceIn(0.08, 1.0).toFloat()

        // 5. Throat (Blue): Resting heart rate alignment + moderate balanced arousal.
        val restingAlignment = (1.0 - (abs(restingHr - targetRestHr) / 30.0)).coerceIn(0.0, 1.0)
        val calmEdaBalance = (1.0 - abs(edaNorm - 0.30) * 1.3).coerceIn(0.0, 1.0)
        val throatIntensity = (restingAlignment * 0.60 + calmEdaBalance * 0.40).coerceIn(0.08, 1.0).toFloat()

        // 6. Third Eye (Indigo): Low-BPM relaxed focus intersecting with Alpha-range calm HRV.
        val lowBpmFocus = (((targetRestHr + 14.0) - hrMean) / 28.0).coerceIn(0.0, 1.0)
        val alphaReadiness = (hrvCoherence * 0.55 + (1.0 - edaNorm) * 0.45).coerceIn(0.0, 1.0)
        val thirdEyeIntensity = (lowBpmFocus * 0.45 + alphaReadiness * 0.55).coerceIn(0.08, 1.0).toFloat()

        // 7. Crown (Violet): Deep relaxation markers (low resting BPM + peak HRV + minimal HR variance).
        val deepCalmBpm = (((targetRestHr + 4.0) - hrMean) / 22.0).coerceIn(0.0, 1.0)
        val crownIntensity = (deepCalmBpm * 0.40 + hrvCoherence * 0.40 + stabilityFactor * 0.20)
            .coerceIn(0.08, 1.0).toFloat()

        val scores = listOf(
            ChakraScore(
                chakra = ChakraType.ROOT,
                intensity = rootIntensity,
                statusLabel = labelFor(rootIntensity),
                metricSnapshot = "HR σ ${format1(hrStdDev)} bpm",
            ),
            ChakraScore(
                chakra = ChakraType.SACRAL,
                intensity = sacralIntensity,
                statusLabel = arousalLabelFor(sacralIntensity),
                metricSnapshot = "EDA ${format1(edaMean)} µS",
            ),
            ChakraScore(
                chakra = ChakraType.SOLAR_PLEXUS,
                intensity = solarIntensity,
                statusLabel = labelFor(solarIntensity),
                metricSnapshot = "Temp ${format1(tempMean)} °C",
            ),
            ChakraScore(
                chakra = ChakraType.HEART,
                intensity = heartIntensity,
                statusLabel = labelFor(heartIntensity),
                metricSnapshot = "HRV ${hrvMean.toInt()} ms",
            ),
            ChakraScore(
                chakra = ChakraType.THROAT,
                intensity = throatIntensity,
                statusLabel = labelFor(throatIntensity),
                metricSnapshot = "Rest ${restingHr.toInt()} bpm",
            ),
            ChakraScore(
                chakra = ChakraType.THIRD_EYE,
                intensity = thirdEyeIntensity,
                statusLabel = labelFor(thirdEyeIntensity),
                metricSnapshot = "Calm ${(alphaReadiness * 100).toInt()}%",
            ),
            ChakraScore(
                chakra = ChakraType.CROWN,
                intensity = crownIntensity,
                statusLabel = labelFor(crownIntensity),
                metricSnapshot = "Still ${(crownIntensity * 100).toInt()}%",
            ),
        )

        val dominant = scores.maxBy { it.intensity }
        val weakest = scores.minBy { it.intensity }
        val balance = scores.map { it.intensity }.average().toFloat().coerceIn(0f, 1f)
        val prescription = routeTherapy(
            weakest = weakest,
            hrMean = hrMean,
            hrStdDev = hrStdDev,
            hrvMean = hrvMean,
            tempMean = tempMean,
            edaMean = edaMean,
        )

        return AuraResult(
            chakras = scores,
            dominantChakra = dominant,
            weakestChakra = weakest,
            overallBalance = balance,
            hrMeanBpm = hrMean,
            hrStdDevBpm = hrStdDev,
            hrvMeanMs = hrvMean,
            skinTempMeanC = tempMean,
            edaMeanMicroS = edaMean,
            prescription = prescription,
            filteredOutlierCount = filteredOutlierCount,
            effectiveSignalQuality = effectiveQuality,
        )
    }

    private fun routeTherapy(
        weakest: ChakraScore,
        hrMean: Double,
        hrStdDev: Double,
        hrvMean: Double,
        tempMean: Double,
        edaMean: Double,
    ): TherapyPrescription {
        val highStressOrUnstable = hrStdDev > 5.0 || edaMean > 7.5 || hrvMean < 32.0
        val lethargicOrCool = tempMean < 33.4 && edaMean < 2.2 && hrMean < 64.0

        return when (weakest.chakra) {
            ChakraType.ROOT -> TherapyPrescription(
                primaryBand = FrequencyBand.AUM,
                secondaryBand = FrequencyBand.ALPHA,
                rationale = "Weakened Root stability (HR variance ±${format1(hrStdDev)} bpm). " +
                    "136.1 Hz Aum harmonic paired with 10 Hz Alpha grounds autonomic rhythm.",
            )
            ChakraType.SACRAL -> {
                if (lethargicOrCool) {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.BETA,
                        secondaryBand = FrequencyBand.GAMMA,
                        rationale = "Low Sacral arousal (EDA ${format1(edaMean)} µS). " +
                            "18 Hz Beta uplifts sluggish energy and sensory vitality.",
                    )
                } else {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.ALPHA,
                        secondaryBand = FrequencyBand.AUM,
                        rationale = "Sacral imbalance detected. 10 Hz Alpha wave restores emotional flow.",
                    )
                }
            }
            ChakraType.SOLAR_PLEXUS -> {
                if (lethargicOrCool || tempMean < 33.8) {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.BETA,
                        secondaryBand = FrequencyBand.GAMMA,
                        rationale = "Low Solar Plexus warmth (${format1(tempMean)} °C) signals lethargy. " +
                            "18 Hz Beta + 40 Hz Gamma stimulates metabolic clarity and drive.",
                    )
                } else {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.GAMMA,
                        secondaryBand = FrequencyBand.BETA,
                        rationale = "Solar Plexus focus tuning via 40 Hz Gamma coherence.",
                    )
                }
            }
            ChakraType.HEART -> TherapyPrescription(
                primaryBand = FrequencyBand.ALPHA,
                secondaryBand = FrequencyBand.AUM,
                rationale = "Low Heart HRV coherence (${hrvMean.toInt()} ms). " +
                    "10 Hz Alpha binaural resonance invites parasympathetic cardiac coherence.",
            )
            ChakraType.THROAT -> TherapyPrescription(
                primaryBand = FrequencyBand.AUM,
                secondaryBand = FrequencyBand.ALPHA,
                rationale = "Throat tension around ${hrMean.toInt()} bpm. " +
                    "136.1 Hz Aum resonance opens vagal breath and calm expression.",
            )
            ChakraType.THIRD_EYE -> {
                if (highStressOrUnstable) {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.ALPHA,
                        secondaryBand = FrequencyBand.AUM,
                        rationale = "Elevated sympathetic tone clouds Third Eye clarity. " +
                            "10 Hz Alpha quiets mental noise before deeper focus.",
                    )
                } else {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.GAMMA,
                        secondaryBand = FrequencyBand.ALPHA,
                        rationale = "Third Eye activation benefits from 40 Hz Gamma insight resonance.",
                    )
                }
            }
            ChakraType.CROWN -> {
                if (highStressOrUnstable) {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.AUM,
                        secondaryBand = FrequencyBand.ALPHA,
                        rationale = "High stress blocks Crown stillness. Ground first with 136.1 Hz Aum and 10 Hz Alpha.",
                    )
                } else {
                    TherapyPrescription(
                        primaryBand = FrequencyBand.GAMMA,
                        secondaryBand = FrequencyBand.ALPHA,
                        rationale = "Crown stillness invites 40 Hz Gamma coherent awareness.",
                    )
                }
            }
        }
    }

    private fun computeStdDev(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        val variance = values.sumOf { (it - mean).pow(2) } / values.size.toDouble()
        return sqrt(variance)
    }

    private fun labelFor(intensity: Float): String = when {
        intensity >= 0.78f -> "Radiant"
        intensity >= 0.56f -> "Balanced"
        intensity >= 0.36f -> "Mild Dip"
        else -> "Depleted"
    }

    private fun arousalLabelFor(intensity: Float): String = when {
        intensity >= 0.75f -> "High Arousal"
        intensity >= 0.45f -> "Engaged"
        else -> "Calm / Low"
    }

    private fun format1(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
}
