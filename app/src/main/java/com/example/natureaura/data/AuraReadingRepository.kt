package com.example.natureaura.data

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.ui.graphics.toArgb
import com.example.natureaura.data.local.AuraDatabase
import com.example.natureaura.data.local.AuraReadingDao
import com.example.natureaura.data.local.AuraReadingEntity
import com.example.natureaura.data.local.PersonalBaselineDao
import com.example.natureaura.data.local.PersonalBaselineEntity
import com.example.natureaura.engine.AuraReadingInterpreter
import com.example.natureaura.engine.AuraReadingNarrative
import com.example.natureaura.engine.AuraReadingRecord
import com.example.natureaura.model.AuraResult
import com.example.natureaura.model.PersonalBaseline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first Repository coordinating Room SQLite persistence, rolling PersonalBaseline
 * calibration, and local Aura Photograph files.
 */
class AuraReadingRepository(
    private val dao: AuraReadingDao,
    private val baselineDao: PersonalBaselineDao,
    private val photoComposer: AuraPhotoComposer,
) {
    constructor(context: Context) : this(
        dao = AuraDatabase.getInstance(context).auraReadingDao(),
        baselineDao = AuraDatabase.getInstance(context).personalBaselineDao(),
        photoComposer = AuraPhotoComposer(context),
    )

    val allReadings: Flow<List<AuraReadingRecord>> = dao.observeAllReadings().map { entities ->
        entities.map { it.toDomainRecord() }
    }

    val personalBaseline: Flow<PersonalBaseline?> = baselineDao.observeBaseline().map { entity ->
        entity?.toDomain()
    }

    suspend fun getBaselineOnce(): PersonalBaseline? {
        return baselineDao.getBaselineOnce()?.toDomain()
    }

    suspend fun resetBaseline() {
        baselineDao.resetBaseline()
    }

    suspend fun saveCaptureAndReading(
        result: AuraResult,
        sourcePlatform: String,
        imageCapture: ImageCapture?,
        parentReadingId: Long? = null,
    ): AuraReadingRecord {
        val narrative = AuraReadingInterpreter.interpret(result)
        val photoPath = photoComposer.captureAndComposeAuraPhoto(
            imageCapture = imageCapture,
            auraResult = result,
            narrative = narrative,
            includePolaroidFrame = true,
        )
        val sorted = result.chakras.sortedByDescending { it.intensity }

        val entity = AuraReadingEntity(
            timestampMs = result.timestampMs,
            photoPath = photoPath,
            sourcePlatform = sourcePlatform,
            parentReadingId = parentReadingId,
            overallBalance = result.overallBalance,
            dominantChakraName = result.dominantChakra.chakra.englishName,
            weakestChakraName = result.weakestChakra.chakra.englishName,
            primaryColorArgb = sorted[0].chakra.color.toArgb(),
            secondaryColorArgb = sorted.getOrElse(1) { sorted[0] }.chakra.color.toArgb(),
            tertiaryColorArgb = sorted.getOrElse(2) { sorted[0] }.chakra.color.toArgb(),
            hrMeanBpm = result.hrMeanBpm,
            hrStdDevBpm = result.hrStdDevBpm,
            hrvMeanMs = result.hrvMeanMs,
            skinTempMeanC = result.skinTempMeanC,
            edaMeanMicroS = result.edaMeanMicroS,
            chakraIntensitiesCsv = result.chakras.joinToString(",") { it.intensity.toString() },
            primaryTherapyBand = result.prescription.primaryBand.name,
            secondaryTherapyBand = result.prescription.secondaryBand?.name,
            therapyRationale = result.prescription.rationale,
            archetypeTitle = narrative.archetypeTitle,
            innerLayerSummary = narrative.innerLayerSummary,
            emotionalLayerSummary = narrative.emotionalLayerSummary,
            outerLayerSummary = narrative.outerLayerSummary,
            readingNarrative = narrative.readingNarrative,
        )

        val insertedId = dao.insertReading(entity)
        updateRollingBaseline(result)
        return entity.copy(id = insertedId).toDomainRecord()
    }

    private suspend fun updateRollingBaseline(result: AuraResult) {
        val current = baselineDao.getBaselineOnce()
        val updated = if (current == null) {
            PersonalBaselineEntity(
                id = 1,
                restingHrBpm = result.hrMeanBpm,
                baselineHrvMs = result.hrvMeanMs,
                baselineSkinTempC = result.skinTempMeanC,
                baselineEdaMicroS = result.edaMeanMicroS,
                scanCount = 1,
                updatedAtMs = result.timestampMs,
            )
        } else {
            val alpha = 0.25
            PersonalBaselineEntity(
                id = 1,
                restingHrBpm = current.restingHrBpm * (1.0 - alpha) + result.hrMeanBpm * alpha,
                baselineHrvMs = current.baselineHrvMs * (1.0 - alpha) + result.hrvMeanMs * alpha,
                baselineSkinTempC = current.baselineSkinTempC * (1.0 - alpha) + result.skinTempMeanC * alpha,
                baselineEdaMicroS = current.baselineEdaMicroS * (1.0 - alpha) + result.edaMeanMicroS * alpha,
                scanCount = current.scanCount + 1,
                updatedAtMs = result.timestampMs,
            )
        }
        baselineDao.upsertBaseline(updated)
    }

    fun getShareablePhotoUri(photoPath: String?): Uri? {
        if (photoPath.isNullOrBlank()) return null
        return photoComposer.getShareableUri(photoPath)
    }

    suspend fun deleteReading(record: AuraReadingRecord) {
        dao.deleteReadingById(record.id)
        photoComposer.deletePhotoFile(record.photoPath)
    }

    private fun PersonalBaselineEntity.toDomain(): PersonalBaseline = PersonalBaseline(
        restingHrBpm = restingHrBpm,
        baselineHrvMs = baselineHrvMs,
        baselineSkinTempC = baselineSkinTempC,
        baselineEdaMicroS = baselineEdaMicroS,
        scanCount = scanCount,
        updatedAtMs = updatedAtMs,
    )

    private fun AuraReadingEntity.toDomainRecord(): AuraReadingRecord {
        val result = AuraReadingInterpreter.reconstructResult(
            timestampMs = timestampMs,
            overallBalance = overallBalance,
            hrMeanBpm = hrMeanBpm,
            hrStdDevBpm = hrStdDevBpm,
            hrvMeanMs = hrvMeanMs,
            skinTempMeanC = skinTempMeanC,
            edaMeanMicroS = edaMeanMicroS,
            intensitiesCsv = chakraIntensitiesCsv,
            primaryBandName = primaryTherapyBand,
            secondaryBandName = secondaryTherapyBand,
            rationale = therapyRationale,
        )
        val enriched = AuraReadingInterpreter.interpret(result)
        return AuraReadingRecord(
            id = id,
            timestampMs = timestampMs,
            photoPath = photoPath,
            sourcePlatform = sourcePlatform,
            parentReadingId = parentReadingId,
            result = result,
            narrative = enriched.copy(
                archetypeTitle = archetypeTitle,
                innerLayerSummary = innerLayerSummary,
                emotionalLayerSummary = emotionalLayerSummary,
                outerLayerSummary = outerLayerSummary,
                readingNarrative = readingNarrative,
            ),
        )
    }
}
