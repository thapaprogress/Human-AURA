package com.example

import com.example.natureaura.engine.AuraCalculator
import com.example.natureaura.model.BiometricPayload
import com.example.natureaura.model.ChakraType
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.PersonalBaseline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuraCalculatorTest {

    private val calculator = AuraCalculator()

    @Test
    fun `unstable HR and high stress weakens Root and routes to Aum and Alpha`() {
        val samples = listOf(
            BiometricPayload("watch-1", "WearOS", 1000L, 78.0, 20.0, 34.5, 11.2),
            BiometricPayload("watch-1", "WearOS", 2000L, 108.0, 18.0, 34.6, 12.4),
            BiometricPayload("watch-1", "WearOS", 3000L, 80.0, 22.0, 34.5, 11.8),
            BiometricPayload("watch-1", "WearOS", 4000L, 112.0, 19.0, 34.6, 13.0),
        )

        val result = calculator.calculate(samples)

        assertEquals(7, result.chakras.size)
        assertEquals(ChakraType.ROOT, result.weakestChakra.chakra)
        assertEquals(FrequencyBand.AUM, result.prescription.primaryBand)
        assertEquals(FrequencyBand.ALPHA, result.prescription.secondaryBand)
    }

    @Test
    fun `cool skin temp and lethargy weakens Solar Plexus and routes to Beta`() {
        val samples = listOf(
            BiometricPayload("watch-1", "WearOS", 1000L, 58.0, 52.0, 31.9, 1.0),
            BiometricPayload("watch-1", "WearOS", 2000L, 59.0, 54.0, 32.0, 1.1),
            BiometricPayload("watch-1", "WearOS", 3000L, 58.5, 53.0, 31.8, 0.9),
        )

        val result = calculator.calculate(samples)

        assertEquals(FrequencyBand.BETA, result.prescription.primaryBand)
    }

    @Test
    fun `peak HRV coherence drives strong Heart chakra`() {
        val samples = listOf(
            BiometricPayload("watch-1", "WearOS", 1000L, 62.0, 95.0, 35.4, 2.5),
            BiometricPayload("watch-1", "WearOS", 2000L, 63.0, 98.0, 35.5, 2.4),
            BiometricPayload("watch-1", "WearOS", 3000L, 62.5, 96.0, 35.4, 2.5),
        )

        val result = calculator.calculate(samples)
        val heart = result.chakras.first { it.chakra == ChakraType.HEART }
        assertTrue("Expected strong Heart coherence >= 0.80, got ${heart.intensity}", heart.intensity >= 0.80f)
    }

    @Test
    fun `motion artifact filter rejects low-quality frames and HR spikes`() {
        val samples = listOf(
            BiometricPayload("watch-1", "WearOS", 1000L, 62.0, 74.0, 35.4, 2.5, signalQuality = 0.95),
            BiometricPayload("watch-1", "WearOS", 2000L, 63.0, 75.0, 35.4, 2.5, signalQuality = 0.96),
            BiometricPayload("watch-1", "WearOS", 3000L, 64.0, 76.0, 35.4, 2.5, signalQuality = 0.94),
            BiometricPayload("watch-1", "WearOS", 4000L, 185.0, 22.0, 35.4, 2.5, signalQuality = 0.90), // >35 bpm spike
            BiometricPayload("watch-1", "WearOS", 5000L, 140.0, 20.0, 35.4, 2.5, signalQuality = 0.20), // low signalQuality
        )

        val result = calculator.calculate(
            samples = samples,
            baseline = PersonalBaseline(63.0, 72.0, 35.4, 2.5, scanCount = 4, updatedAtMs = 5000L),
        )

        assertEquals(2, result.filteredOutlierCount)
        assertTrue(result.hrMeanBpm in 60.0..66.0)
    }

    @Test
    fun `biometric payload JSON round-trip preserves telemetry`() {
        val original = BiometricPayload(
            deviceId = "galaxy-watch-7",
            sourcePlatform = "WearOS",
            timestampMs = 1730000000000L,
            heartRateBpm = 66.5,
            hrvSdnnMs = 74.2,
            skinTempCelsius = 35.4,
            edaMicroSiemens = 3.1,
            restingHeartRateBpm = 61.0,
            signalQuality = 0.98,
        )

        val parsed = BiometricPayload.fromJsonString(original.toJsonString())
        assertEquals(original.deviceId, parsed.deviceId)
        assertEquals(original.heartRateBpm, parsed.heartRateBpm, 0.01)
        assertEquals(original.hrvSdnnMs, parsed.hrvSdnnMs, 0.01)
        assertEquals(original.skinTempCelsius, parsed.skinTempCelsius, 0.01)
        assertEquals(original.edaMicroSiemens, parsed.edaMicroSiemens, 0.01)
    }
}
