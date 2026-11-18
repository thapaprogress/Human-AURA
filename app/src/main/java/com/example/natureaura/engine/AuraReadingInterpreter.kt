package com.example.natureaura.engine

import androidx.compose.ui.graphics.Color
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.BirthChartProfile
import com.example.natureaura.model.ChakraScore
import com.example.natureaura.model.ChakraType
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.TherapyPrescription
import java.util.Locale

data class AuraReadingNarrative(
    val archetypeTitle: String,
    val energeticEssenceTag: String,
    val innerLayerSummary: String,
    val emotionalLayerSummary: String,
    val outerLayerSummary: String,
    val readingNarrative: String,
    val ascendantTitle: String,
    val ascendantSummary: String,
    val descendantTitle: String,
    val descendantSummary: String,
    val personalityInsight: String,
    val loveInsight: String,
    val careerInsight: String,
    val dailyMantra: String,
)

data class AuraReadingRecord(
    val id: Long = 0L,
    val timestampMs: Long,
    val photoPath: String?,
    val sourcePlatform: String,
    val parentReadingId: Long? = null,
    val result: AuraResult,
    val narrative: AuraReadingNarrative,
)

data class AuraDeltaComparison(
    val harmonyDeltaPercent: Int,
    val hrvDeltaMs: Int,
    val hrDeltaBpm: Int,
    val edaDeltaMicroS: Double,
    val shiftedDominant: Boolean,
    val summary: String,
)

data class AuraColorSlice(
    val chakra: ChakraType,
    val colorName: String,
    val percentage: Int,
    val color: Color,
    val meaning: String,
)

/**
 * Generates an Aurla-inspired layered aura reading (Archetype, Ascendant & Descendant Polarity,
 * Inner/Emotional/Outer Layers, Personality, Love, Career, Daily Mantra, and Delta Comparison)
 * directly from AuraCalculator output and optional offline BirthChartProfile.
 */
object AuraReadingInterpreter {

    private val zodiacSigns = listOf(
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
    )

    fun interpret(
        result: AuraResult,
        birthProfile: BirthChartProfile? = null,
    ): AuraReadingNarrative {
        val sorted = result.chakras.sortedByDescending { it.intensity }
        val dominant = sorted[0]
        val secondary = sorted[1]
        val weakest = result.weakestChakra

        val archetypeTitle = when (dominant.chakra) {
            ChakraType.ROOT -> "Crimson Grounded Force"
            ChakraType.SACRAL -> "Amber Kinetic Flame"
            ChakraType.SOLAR_PLEXUS -> "Solar Citrine Radiance"
            ChakraType.HEART -> "Emerald Heart Coherence"
            ChakraType.THROAT -> "Cerulean Lucid Current"
            ChakraType.THIRD_EYE -> "Indigo Intuitive Mirror"
            ChakraType.CROWN -> "Violet Stillness Crown"
        }

        val energeticEssenceTag = "${dominant.chakra.colorName} Lead · ${secondary.chakra.colorName} Undertone"

        val rootScore = result.chakras.first { it.chakra == ChakraType.ROOT }
        val sacralScore = result.chakras.first { it.chakra == ChakraType.SACRAL }
        val solarScore = result.chakras.first { it.chakra == ChakraType.SOLAR_PLEXUS }
        val heartScore = result.chakras.first { it.chakra == ChakraType.HEART }
        val throatScore = result.chakras.first { it.chakra == ChakraType.THROAT }
        val thirdEyeScore = result.chakras.first { it.chakra == ChakraType.THIRD_EYE }
        val crownScore = result.chakras.first { it.chakra == ChakraType.CROWN }

        val innerAvg = ((rootScore.intensity + solarScore.intensity) / 2f * 100).toInt()
        val emotionalAvg = ((sacralScore.intensity + heartScore.intensity) / 2f * 100).toInt()
        val outerAvg = ((throatScore.intensity + thirdEyeScore.intensity + crownScore.intensity) / 3f * 100).toInt()

        val innerLayerSummary = "Inner Core ($innerAvg%): Driven by ${rootScore.metricSnapshot} and " +
            "${solarScore.metricSnapshot}. " +
            if (innerAvg >= 65) {
                "Your physical baseline is steady and metabolically warm."
            } else {
                "Autonomic rhythm shows baseline variability; grounding breath will steady your core."
            }

        val emotionalLayerSummary = "Emotional Field ($emotionalAvg%): Reflects ${heartScore.metricSnapshot} and " +
            "${sacralScore.metricSnapshot}. " +
            if (heartScore.intensity >= 0.65f) {
                "Strong vagal HRV coherence creates an open, restorative emerald field."
            } else {
                "Sympathetic arousal is elevated; gentle parasympathetic pacing restores flow."
            }

        val outerLayerSummary = "Outer Crown ($outerAvg%): Resting alignment (${throatScore.metricSnapshot}) " +
            "with ${crownScore.metricSnapshot}. " +
            if (outerAvg >= 65) {
                "Mental clarity and meditative stillness radiate into the outer halo."
            } else {
                "Cognitive tension narrows the outer field; resonant toning can widen clarity."
            }

        val narrative = buildString {
            append(
                "Your aura leads with ${dominant.chakra.colorName} (${dominant.chakra.englishName}, ${(dominant.intensity * 100).toInt()}%) ",
            )
            append(
                "woven with ${secondary.chakra.colorName} (${(secondary.intensity * 100).toInt()}%), ",
            )
            append(
                "yielding ${(result.overallBalance * 100).toInt()}% biofield harmony across a ",
            )
            append(
                "${result.hrMeanBpm.toInt()} bpm pulse and ${String.format(Locale.US, "%.1f", result.skinTempMeanC)}°C warmth. ",
            )
            append(
                "Your ${weakest.chakra.englishName} center (${(weakest.intensity * 100).toInt()}%) is your growth doorway—",
            )
            append(weakest.chakra.healingGuidance)
        }

        val zodiacAxis = birthProfile?.let { computeZodiacAxis(it) }

        // Ascendant = How your dominant radiating aura projects outward to the world
        val ascendantBaseTitle = when (dominant.chakra) {
            ChakraType.ROOT -> "Magnetic Anchor (${dominant.chakra.colorName} Rising)"
            ChakraType.SACRAL -> "Radiant Catalyst (${dominant.chakra.colorName} Rising)"
            ChakraType.SOLAR_PLEXUS -> "Solar Vanguard (${dominant.chakra.colorName} Rising)"
            ChakraType.HEART -> "Harmonizing Empath (${dominant.chakra.colorName} Rising)"
            ChakraType.THROAT -> "Lucid Messenger (${dominant.chakra.colorName} Rising)"
            ChakraType.THIRD_EYE -> "Visionary Seer (${dominant.chakra.colorName} Rising)"
            ChakraType.CROWN -> "Serene Luminary (${dominant.chakra.colorName} Rising)"
        }
        val ascendantTitle = if (zodiacAxis != null) {
            "$ascendantBaseTitle · ${zodiacAxis.rising} Rising"
        } else {
            ascendantBaseTitle
        }
        val ascendantSummary = buildString {
            if (zodiacAxis != null) {
                append("${zodiacAxis.sun} Sun with ${zodiacAxis.rising} Ascendant amplifies your outer aura. ")
            }
            append(
                when (dominant.chakra) {
                    ChakraType.ROOT -> "Others immediately sense calm physical presence, reliability, and unshakeable stamina."
                    ChakraType.SACRAL -> "You enter rooms with expressive warmth, creative spark, and tactile emotional magnetism."
                    ChakraType.SOLAR_PLEXUS -> "Your first impression radiates decisive warmth, self-assurance, and bright mental momentum."
                    ChakraType.HEART -> "People experience immediate safety, empathy, and physiological ease in your presence."
                    ChakraType.THROAT -> "Your outward signature carries clear articulation, rhythmic poise, and candid composure."
                    ChakraType.THIRD_EYE -> "Observers notice perceptive stillness, quiet depth, and intuitive pattern awareness."
                    ChakraType.CROWN -> "Your outer corona projects serene detachment, clarity, and spacious contemplative calm."
                },
            )
        }

        // Descendant = Polar complementary / receptive axis showing what you attract in love & shadow balance
        val complementaryChakra = complementaryAxisFor(dominant.chakra, weakest.chakra)
        val descendantBaseTitle = when (complementaryChakra) {
            ChakraType.ROOT -> "Earthly Steadiness (${complementaryChakra.colorName} Polarity)"
            ChakraType.SACRAL -> "Sensory Spark (${complementaryChakra.colorName} Polarity)"
            ChakraType.SOLAR_PLEXUS -> "Radiant Confidence (${complementaryChakra.colorName} Polarity)"
            ChakraType.HEART -> "Devoted Warmth (${complementaryChakra.colorName} Polarity)"
            ChakraType.THROAT -> "Candid Resonance (${complementaryChakra.colorName} Polarity)"
            ChakraType.THIRD_EYE -> "Reflective Mystery (${complementaryChakra.colorName} Polarity)"
            ChakraType.CROWN -> "Transcendent Grace (${complementaryChakra.colorName} Polarity)"
        }
        val descendantTitle = if (zodiacAxis != null) {
            "$descendantBaseTitle · ${zodiacAxis.descendant} 7th House"
        } else {
            descendantBaseTitle
        }
        val descendantSummary = buildString {
            if (zodiacAxis != null) {
                append("${zodiacAxis.descendant} Descendant guides your relational mirror. ")
            }
            append(
                "Sitting opposite your outward projection, your Descendant attracts ${complementaryChakra.colorName.lowercase()} qualities in partners and close allies—",
            )
            append(
                when (complementaryChakra) {
                    ChakraType.ROOT -> "drawn toward grounded, practical companions who anchor your lofty vision into daily reality."
                    ChakraType.SACRAL -> "drawn toward spontaneous, emotionally vivid partners who awaken joy and play."
                    ChakraType.SOLAR_PLEXUS -> "drawn toward bold, action-oriented allies who spark initiative and mutual courage."
                    ChakraType.HEART -> "drawn toward deeply compassionate partners who invite vulnerability and shared calm."
                    ChakraType.THROAT -> "drawn toward articulate truth-tellers who value open dialogue and active listening."
                    ChakraType.THIRD_EYE -> "drawn toward perceptive, soulful minds who mirror your unspoken intuition."
                    ChakraType.CROWN -> "drawn toward serene, wisdom-seeking companions who honor quiet stillness together."
                },
            )
        }

        val personalityInsight = when (dominant.chakra) {
            ChakraType.ROOT -> "Strengths: Endurance, pragmatic calm under pressure, and somatic intuition. Watch-out: Over-guarding when heart rate variability dips."
            ChakraType.SACRAL -> "Strengths: Emotional fluency, creative originality, and sensory presence. Watch-out: Absorbing stress spikes from fast-paced rooms."
            ChakraType.SOLAR_PLEXUS -> "Strengths: Executive clarity, optimistic drive, and warm leadership. Watch-out: Pushing stamina past your natural rest cadence."
            ChakraType.HEART -> "Strengths: High autonomic coherence, natural peacemaking, and restorative empathy. Watch-out: Carrying others' emotional weight."
            ChakraType.THROAT -> "Strengths: Honest communication, balanced vagal tone, and artistic timing. Watch-out: Silencing creative impulses to keep peace."
            ChakraType.THIRD_EYE -> "Strengths: Pattern synthesis, focused introspection, and calm alertness. Watch-out: Over-analyzing instead of resting in the body."
            ChakraType.CROWN -> "Strengths: Big-picture synthesis, deep parasympathetic stillness, and meditative poise. Watch-out: Feeling ungrounded during busy tasks."
        }

        val loveInsight = "Best aura resonance with ${complementaryChakra.colorName} and ${secondary.chakra.colorName} fields. " +
            if (heartScore.intensity >= 0.65f) {
                "Your Heart coherence (${(heartScore.intensity * 100).toInt()}%) invites effortless intimacy and mutual regulation."
            } else {
                "Before deep conversations, 3 minutes of coherent breathing opens your ${weakest.chakra.englishName.lowercase()} receptivity."
            }

        val careerInsight = when {
            solarScore.intensity >= 0.68f || dominant.chakra == ChakraType.SOLAR_PLEXUS ->
                "Peak momentum for leadership, pitching, and decisive execution—channel your warm drive into structured milestones."
            thirdEyeScore.intensity >= 0.68f || crownScore.intensity >= 0.68f ->
                "High clarity window for deep research, design strategy, and independent creative synthesis away from distractions."
            else ->
                "Collaborative, people-centered workflows suit today's rhythm best; alternate focused sprints with brief grounding breaks."
        }

        val dailyMantra = when (weakest.chakra) {
            ChakraType.ROOT -> "“I am anchored in steady breath; every pulse restores my foundation.”"
            ChakraType.SACRAL -> "“I let creative energy move freely without holding tension.”"
            ChakraType.SOLAR_PLEXUS -> "“Warm clarity rises within me; my focus is calm and bright.”"
            ChakraType.HEART -> "“With every exhale, my heart field opens in steady coherence.”"
            ChakraType.THROAT -> "“My voice flows with quiet truth and effortless ease.”"
            ChakraType.THIRD_EYE -> "“I trust clear perception beyond noise and hurry.”"
            ChakraType.CROWN -> "“Stillness surrounds me; I rest in spacious awareness.”"
        }

        return AuraReadingNarrative(
            archetypeTitle = archetypeTitle,
            energeticEssenceTag = energeticEssenceTag,
            innerLayerSummary = innerLayerSummary,
            emotionalLayerSummary = emotionalLayerSummary,
            outerLayerSummary = outerLayerSummary,
            readingNarrative = narrative,
            ascendantTitle = ascendantTitle,
            ascendantSummary = ascendantSummary,
            descendantTitle = descendantTitle,
            descendantSummary = descendantSummary,
            personalityInsight = personalityInsight,
            loveInsight = loveInsight,
            careerInsight = careerInsight,
            dailyMantra = dailyMantra,
        )
    }

    private fun complementaryAxisFor(dominant: ChakraType, weakest: ChakraType): ChakraType {
        if (weakest != dominant) return weakest
        return when (dominant) {
            ChakraType.ROOT -> ChakraType.CROWN
            ChakraType.SACRAL -> ChakraType.THIRD_EYE
            ChakraType.SOLAR_PLEXUS -> ChakraType.THROAT
            ChakraType.HEART -> ChakraType.SOLAR_PLEXUS
            ChakraType.THROAT -> ChakraType.SOLAR_PLEXUS
            ChakraType.THIRD_EYE -> ChakraType.SACRAL
            ChakraType.CROWN -> ChakraType.ROOT
        }
    }

    private data class ZodiacAxis(val sun: String, val rising: String, val descendant: String)

    private fun computeZodiacAxis(profile: BirthChartProfile): ZodiacAxis {
        val m = profile.birthMonth.coerceIn(1, 12)
        val d = profile.birthDay.coerceIn(1, 31)
        val sunIdx = when {
            (m == 3 && d >= 21) || (m == 4 && d <= 19) -> 0
            (m == 4 && d >= 20) || (m == 5 && d <= 20) -> 1
            (m == 5 && d >= 21) || (m == 6 && d <= 20) -> 2
            (m == 6 && d >= 21) || (m == 7 && d <= 22) -> 3
            (m == 7 && d >= 23) || (m == 8 && d <= 22) -> 4
            (m == 8 && d >= 23) || (m == 9 && d <= 22) -> 5
            (m == 9 && d >= 23) || (m == 10 && d <= 22) -> 6
            (m == 10 && d >= 23) || (m == 11 && d <= 21) -> 7
            (m == 11 && d >= 22) || (m == 12 && d <= 21) -> 8
            (m == 12 && d >= 22) || (m == 1 && d <= 19) -> 9
            (m == 1 && d >= 20) || (m == 2 && d <= 18) -> 10
            else -> 11
        }
        val hourShift = ((profile.birthHour24.coerceIn(0, 23) - 6 + 24) % 24) / 2
        val risingIdx = (sunIdx + hourShift) % 12
        val descIdx = (risingIdx + 6) % 12
        return ZodiacAxis(
            sun = zodiacSigns[sunIdx],
            rising = zodiacSigns[risingIdx],
            descendant = zodiacSigns[descIdx],
        )
    }

    fun computeDelta(before: AuraReadingRecord, after: AuraReadingRecord): AuraDeltaComparison {
        val harmonyDelta = ((after.result.overallBalance - before.result.overallBalance) * 100).toInt()
        val hrvDelta = (after.result.hrvMeanMs - before.result.hrvMeanMs).toInt()
        val hrDelta = (after.result.hrMeanBpm - before.result.hrMeanBpm).toInt()
        val edaDelta = after.result.edaMeanMicroS - before.result.edaMeanMicroS
        val shifted = before.result.dominantChakra.chakra != after.result.dominantChakra.chakra

        val summary = buildString {
            append("Harmony ")
            append(if (harmonyDelta >= 0) "+$harmonyDelta%" else "$harmonyDelta%")
            append(" · HRV ")
            append(if (hrvDelta >= 0) "+${hrvDelta}ms" else "${hrvDelta}ms")
            if (shifted) {
                append(" · Shifted from ${before.result.dominantChakra.chakra.englishName} to ${after.result.dominantChakra.chakra.englishName}")
            }
        }

        return AuraDeltaComparison(
            harmonyDeltaPercent = harmonyDelta,
            hrvDeltaMs = hrvDelta,
            hrDeltaBpm = hrDelta,
            edaDeltaMicroS = edaDelta,
            shiftedDominant = shifted,
            summary = summary,
        )
    }

    fun reconstructResult(
        timestampMs: Long,
        overallBalance: Float,
        hrMeanBpm: Double,
        hrStdDevBpm: Double,
        hrvMeanMs: Double,
        skinTempMeanC: Double,
        edaMeanMicroS: Double,
        intensitiesCsv: String,
        primaryBandName: String,
        secondaryBandName: String?,
        rationale: String,
    ): AuraResult {
        val values = intensitiesCsv.split(",").mapNotNull { it.toFloatOrNull() }
        val scores = ChakraType.entries.mapIndexed { idx, chakra ->
            val intensity = values.getOrElse(idx) { 0.5f }.coerceIn(0f, 1f)
            val snapshot = when (chakra) {
                ChakraType.ROOT -> "HR σ ${String.format(Locale.US, "%.1f", hrStdDevBpm)} bpm"
                ChakraType.SACRAL -> "EDA ${String.format(Locale.US, "%.1f", edaMeanMicroS)} µS"
                ChakraType.SOLAR_PLEXUS -> "Temp ${String.format(Locale.US, "%.1f", skinTempMeanC)} °C"
                ChakraType.HEART -> "HRV ${hrvMeanMs.toInt()} ms"
                ChakraType.THROAT -> "Rest ${hrMeanBpm.toInt()} bpm"
                ChakraType.THIRD_EYE -> "Calm ${(intensity * 100).toInt()}%"
                ChakraType.CROWN -> "Still ${(intensity * 100).toInt()}%"
            }
            ChakraScore(
                chakra = chakra,
                intensity = intensity,
                statusLabel = when {
                    intensity >= 0.78f -> "Radiant"
                    intensity >= 0.56f -> "Balanced"
                    intensity >= 0.36f -> "Mild Dip"
                    else -> "Depleted"
                },
                metricSnapshot = snapshot,
            )
        }

        val dominant = scores.maxBy { it.intensity }
        val weakest = scores.minBy { it.intensity }
        val primaryBand = runCatching { FrequencyBand.valueOf(primaryBandName) }.getOrDefault(FrequencyBand.AUM)
        val secondaryBand = secondaryBandName?.let { runCatching { FrequencyBand.valueOf(it) }.getOrNull() }

        return AuraResult(
            chakras = scores,
            dominantChakra = dominant,
            weakestChakra = weakest,
            overallBalance = overallBalance,
            hrMeanBpm = hrMeanBpm,
            hrStdDevBpm = hrStdDevBpm,
            hrvMeanMs = hrvMeanMs,
            skinTempMeanC = skinTempMeanC,
            edaMeanMicroS = edaMeanMicroS,
            prescription = TherapyPrescription(
                primaryBand = primaryBand,
                secondaryBand = secondaryBand,
                rationale = rationale,
            ),
            timestampMs = timestampMs,
        )
    }

    fun computeColorSignature(result: AuraResult): List<AuraColorSlice> {
        val sorted = result.chakras.sortedByDescending { it.intensity }
        val total = sorted.sumOf { it.intensity.toDouble().coerceAtLeast(0.05) }.coerceAtLeast(0.1)
        var runningPct = 0
        return sorted.mapIndexed { idx, score ->
            val pct = if (idx == sorted.lastIndex) {
                (100 - runningPct).coerceAtLeast(1)
            } else {
                ((score.intensity / total) * 100.0).toInt().coerceAtLeast(1).also {
                    runningPct += it
                }
            }
            val meaning = when (score.chakra) {
                ChakraType.ROOT -> "Vital grounding, physical stamina, and autonomic steadiness."
                ChakraType.SACRAL -> "Warm kinetic creativity, sensory expression, and emotional drive."
                ChakraType.SOLAR_PLEXUS -> "Radiant personal power, metabolic warmth, and confident clarity."
                ChakraType.HEART -> "Compassion, vagal HRV coherence, and restorative emotional balance."
                ChakraType.THROAT -> "Lucid expression, calm resting tone, and resonant communication."
                ChakraType.THIRD_EYE -> "Intuitive focus, relaxed alertness, and clear pattern perception."
                ChakraType.CROWN -> "Deep stillness, meditative coherence, and spacious awareness."
            }
            AuraColorSlice(
                chakra = score.chakra,
                colorName = score.chakra.colorName,
                percentage = pct,
                color = score.chakra.color,
                meaning = meaning,
            )
        }
    }

    fun colorFromArgb(argb: Int): Color = Color(argb)
}
