package com.example.natureaura.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aura_readings")
data class AuraReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long,
    val photoPath: String?,
    val sourcePlatform: String,
    val parentReadingId: Long? = null,
    val overallBalance: Float,
    val dominantChakraName: String,
    val weakestChakraName: String,
    val primaryColorArgb: Int,
    val secondaryColorArgb: Int,
    val tertiaryColorArgb: Int,
    val hrMeanBpm: Double,
    val hrStdDevBpm: Double,
    val hrvMeanMs: Double,
    val skinTempMeanC: Double,
    val edaMeanMicroS: Double,
    val chakraIntensitiesCsv: String,
    val primaryTherapyBand: String,
    val secondaryTherapyBand: String?,
    val therapyRationale: String,
    val archetypeTitle: String,
    val innerLayerSummary: String,
    val emotionalLayerSummary: String,
    val outerLayerSummary: String,
    val readingNarrative: String,
)

@Entity(tableName = "personal_baselines")
data class PersonalBaselineEntity(
    @PrimaryKey val id: Int = 1,
    val restingHrBpm: Double,
    val baselineHrvMs: Double,
    val baselineSkinTempC: Double,
    val baselineEdaMicroS: Double,
    val scanCount: Int,
    val updatedAtMs: Long,
)
