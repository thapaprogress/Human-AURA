package com.example.natureaura.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ChakraCrown
import com.example.ui.theme.ChakraHeart
import com.example.ui.theme.ChakraRoot
import com.example.ui.theme.ChakraSacral
import com.example.ui.theme.ChakraSolar
import com.example.ui.theme.ChakraThirdEye
import com.example.ui.theme.ChakraThroat
import org.json.JSONObject

enum class TelemetrySourceMode(val displayName: String) {
    NONE("No Source Active"),
    ON_DEVICE_SENSORS("Phone Sensors (No Preset)"),
    WEAR_OS_DATA_LAYER("Wear OS Data Layer"),
    BLE_GATT("BLE Biometric Sensor"),
    CAMERA_PPG("Rear Finger-PPG"),
    CALIBRATION_SIGNAL("Calibration Preset"),
}

enum class CalibrationPreset(
    val title: String,
    val subtitle: String,
    val baseBpm: Double,
    val baseHrvSdnn: Double,
    val baseSkinTempC: Double,
    val baseEdaMicroS: Double,
    val hrJitter: Double,
) {
    GROUNDED_CALM(
        title = "Balanced Coherence",
        subtitle = "Steady HR 62 bpm · HRV 76 ms · Warm 35.6°C",
        baseBpm = 62.0,
        baseHrvSdnn = 76.0,
        baseSkinTempC = 35.6,
        baseEdaMicroS = 2.4,
        hrJitter = 1.4,
    ),
    STRESSED_LOW_ROOT(
        title = "Acute Stress (Low Root)",
        subtitle = "Erratic HR 94 bpm · HRV 22 ms · High EDA 12.5 µS",
        baseBpm = 94.0,
        baseHrvSdnn = 22.0,
        baseSkinTempC = 33.4,
        baseEdaMicroS = 12.5,
        hrJitter = 9.5,
    ),
    LETHARGIC_LOW_SOLAR(
        title = "Low Vitality (Lethargy)",
        subtitle = "Cool 32.3°C · Flat HRV 31 ms · Low EDA 0.9 µS",
        baseBpm = 56.0,
        baseHrvSdnn = 31.0,
        baseSkinTempC = 32.3,
        baseEdaMicroS = 0.9,
        hrJitter = 2.2,
    ),
    DEEP_MEDITATION(
        title = "Deep Crown Stillness",
        subtitle = "Resting HR 50 bpm · Peak HRV 96 ms · Calm EDA 1.5 µS",
        baseBpm = 50.0,
        baseHrvSdnn = 96.0,
        baseSkinTempC = 35.9,
        baseEdaMicroS = 1.5,
        hrJitter = 0.9,
    ),
}

data class BiometricPayload(
    val deviceId: String,
    val sourcePlatform: String,
    val timestampMs: Long,
    val heartRateBpm: Double,
    val hrvSdnnMs: Double,
    val skinTempCelsius: Double,
    val edaMicroSiemens: Double,
    val restingHeartRateBpm: Double? = null,
    val signalQuality: Double = 1.0,
) {
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("device_id", deviceId)
        obj.put("source_platform", sourcePlatform)
        obj.put("timestamp_ms", timestampMs)
        obj.put("heart_rate_bpm", heartRateBpm)
        obj.put("hrv_sdnn_ms", hrvSdnnMs)
        obj.put("skin_temp_celsius", skinTempCelsius)
        obj.put("eda_micro_siemens", edaMicroSiemens)
        if (restingHeartRateBpm != null) {
            obj.put("resting_heart_rate_bpm", restingHeartRateBpm)
        }
        obj.put("signal_quality", signalQuality)
        return obj.toString(2)
    }

    companion object {
        fun fromJsonString(rawJson: String): BiometricPayload {
            val obj = JSONObject(rawJson)
            return BiometricPayload(
                deviceId = obj.optString("device_id", "wearable-node"),
                sourcePlatform = obj.optString("source_platform", "WearOS"),
                timestampMs = obj.optLong("timestamp_ms", System.currentTimeMillis()),
                heartRateBpm = obj.getDouble("heart_rate_bpm"),
                hrvSdnnMs = obj.optDouble("hrv_sdnn_ms", 45.0),
                skinTempCelsius = obj.optDouble("skin_temp_celsius", 34.5),
                edaMicroSiemens = obj.optDouble("eda_micro_siemens", 3.0),
                restingHeartRateBpm = if (obj.has("resting_heart_rate_bpm")) {
                    obj.optDouble("resting_heart_rate_bpm")
                } else {
                    null
                },
                signalQuality = obj.optDouble("signal_quality", 1.0),
            )
        }
    }
}

data class PersonalBaseline(
    val restingHrBpm: Double,
    val baselineHrvMs: Double,
    val baselineSkinTempC: Double,
    val baselineEdaMicroS: Double,
    val scanCount: Int,
    val updatedAtMs: Long,
)

enum class ChakraType(
    val sanskritName: String,
    val englishName: String,
    val colorName: String,
    val color: Color,
    val rgbNormalized: Triple<Float, Float, Float>,
    val primaryDriver: String,
    val healingGuidance: String,
) {
    ROOT(
        sanskritName = "Muladhara",
        englishName = "Root",
        colorName = "Crimson Ruby",
        color = ChakraRoot,
        rgbNormalized = Triple(1.0f, 0.18f, 0.36f),
        primaryDriver = "Heart Rate Stability (Variance)",
        healingGuidance = "Grounding breathwork & 136.1 Hz Aum resonance stabilize baseline autonomic rhythm.",
    ),
    SACRAL(
        sanskritName = "Svadhisthana",
        englishName = "Sacral",
        colorName = "Radiant Amber",
        color = ChakraSacral,
        rgbNormalized = Triple(1.0f, 0.45f, 0.12f),
        primaryDriver = "Electrodermal Activity & Stress Arousal",
        healingGuidance = "Alpha waves soothe sympathetic skin conductance spikes and emotional reactivity.",
    ),
    SOLAR_PLEXUS(
        sanskritName = "Manipura",
        englishName = "Solar Plexus",
        colorName = "Saffron Gold",
        color = ChakraSolar,
        rgbNormalized = Triple(1.0f, 0.83f, 0.18f),
        primaryDriver = "Skin Temperature & Steady Pulse",
        healingGuidance = "Beta & Gamma harmonic tone restores peripheral warmth, vitality, and focus.",
    ),
    HEART(
        sanskritName = "Anahata",
        englishName = "Heart",
        colorName = "Electric Emerald",
        color = ChakraHeart,
        rgbNormalized = Triple(0.12f, 0.88f, 0.54f),
        primaryDriver = "Peak HRV Coherence (SDNN)",
        healingGuidance = "Heart-brain synchronization deepens with resonant 10 Hz Alpha breathing.",
    ),
    THROAT(
        sanskritName = "Vishuddha",
        englishName = "Throat",
        colorName = "Cerulean Cyan",
        color = ChakraThroat,
        rgbNormalized = Triple(0.10f, 0.72f, 1.0f),
        primaryDriver = "Resting Pulse & Calm Tone",
        healingGuidance = "Vocal toning with Aum vibrations releases vagal tension and restores expression.",
    ),
    THIRD_EYE(
        sanskritName = "Ajna",
        englishName = "Third Eye",
        colorName = "Royal Indigo",
        color = ChakraThirdEye,
        rgbNormalized = Triple(0.38f, 0.30f, 1.0f),
        primaryDriver = "Low-BPM Alpha Intersection",
        healingGuidance = "Alpha-Gamma crossover sharpens intuitive clarity without sympathetic strain.",
    ),
    CROWN(
        sanskritName = "Sahasrara",
        englishName = "Crown",
        colorName = "Ultraviolet Magenta",
        color = ChakraCrown,
        rgbNormalized = Triple(0.78f, 0.32f, 1.0f),
        primaryDriver = "Deep Stillness & Parasympathetic Peak",
        healingGuidance = "40 Hz Gamma stillness nurtures elevated coherence and serene awareness.",
    ),
}

data class ChakraScore(
    val chakra: ChakraType,
    val intensity: Float,
    val statusLabel: String,
    val metricSnapshot: String,
)

enum class FrequencyBand(
    val title: String,
    val frequencyLabel: String,
    val carrierHz: Double,
    val beatHz: Double,
    val isBinaural: Boolean,
    val accentColor: Color,
    val summary: String,
) {
    AUM(
        title = "Aum Grounding",
        frequencyLabel = "136.1 Hz",
        carrierHz = 136.1,
        beatHz = 0.0,
        isBinaural = false,
        accentColor = ChakraRoot,
        summary = "Cosmic Earth-year harmonic overtones for somatic grounding & vagal tone.",
    ),
    ALPHA(
        title = "Alpha Calm",
        frequencyLabel = "10.0 Hz",
        carrierHz = 200.0,
        beatHz = 10.0,
        isBinaural = true,
        accentColor = ChakraHeart,
        summary = "Restorative meditative bridge easing HRV stress and sympathetic arousal.",
    ),
    BETA(
        title = "Beta Vitality",
        frequencyLabel = "18.0 Hz",
        carrierHz = 220.0,
        beatHz = 18.0,
        isBinaural = true,
        accentColor = ChakraSolar,
        summary = "Gentle activating rhythm countering lethargy, cool extremities, and fog.",
    ),
    GAMMA(
        title = "Gamma Coherence",
        frequencyLabel = "40.0 Hz",
        carrierHz = 240.0,
        beatHz = 40.0,
        isBinaural = true,
        accentColor = ChakraCrown,
        summary = "High-coherence neural binding wave supporting clarity and peak restoration.",
    ),
}

enum class BreathPaceMode(
    val title: String,
    val inhaleSec: Float,
    val holdSec: Float,
    val exhaleSec: Float,
    val holdAfterSec: Float,
) {
    COHERENT_5_5("Coherent 5.5s", 5.5f, 0f, 5.5f, 0f),
    GROUNDING_4_7_8("Grounding 4-7-8", 4f, 7f, 8f, 0f),
    BOX_4_4_4_4("Box 4-4-4-4", 4f, 4f, 4f, 4f),
}

enum class SleepTimerOption(val label: String, val durationSeconds: Int?) {
    CONTINUOUS("Continuous", null),
    FIVE_MIN("5 min", 5 * 60),
    FIFTEEN_MIN("15 min", 15 * 60),
    THIRTY_MIN("30 min", 30 * 60),
}

data class TherapyPrescription(
    val primaryBand: FrequencyBand,
    val secondaryBand: FrequencyBand?,
    val rationale: String,
)

data class AuraResult(
    val chakras: List<ChakraScore>,
    val dominantChakra: ChakraScore,
    val weakestChakra: ChakraScore,
    val overallBalance: Float,
    val hrMeanBpm: Double,
    val hrStdDevBpm: Double,
    val hrvMeanMs: Double,
    val skinTempMeanC: Double,
    val edaMeanMicroS: Double,
    val prescription: TherapyPrescription,
    val filteredOutlierCount: Int = 0,
    val effectiveSignalQuality: Double = 1.0,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class WearableNodeInfo(
    val id: String,
    val displayName: String,
    val isNearby: Boolean,
)

data class BleSensorDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val isConnected: Boolean = false,
)

data class BirthChartProfile(
    val birthMonth: Int,
    val birthDay: Int,
    val birthHour24: Int,
)

data class SensorCapabilityReport(
    val hasFingerprintHardware: Boolean = false,
    val isFingerprintEnrolled: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasAmbientTempSensor: Boolean = false,
    val hasLightSensor: Boolean = false,
    val hasHardwareHeartRate: Boolean = false,
)
