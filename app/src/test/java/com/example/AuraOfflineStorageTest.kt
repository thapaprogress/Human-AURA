package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.natureaura.data.AuraPhotoComposer
import com.example.natureaura.data.AuraReadingRepository
import com.example.natureaura.data.local.AuraDatabase
import com.example.natureaura.engine.AuraCalculator
import com.example.natureaura.engine.AuraReadingInterpreter
import com.example.natureaura.model.BiometricPayload
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuraOfflineStorageTest {

    private lateinit var context: Context
    private lateinit var db: AuraDatabase
    private lateinit var repository: AuraReadingRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AuraReadingRepository(
            dao = db.auraReadingDao(),
            baselineDao = db.personalBaselineDao(),
            photoComposer = AuraPhotoComposer(context),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `interpreter generates layered aura reading and delta comparison`() {
        val calc = AuraCalculator()
        val beforeResult = calc.calculate(
            listOf(
                BiometricPayload("w1", "WearOS", 1000L, 92.0, 24.0, 33.8, 10.2),
                BiometricPayload("w1", "WearOS", 2000L, 84.0, 26.0, 33.9, 9.8),
            ),
        )
        val afterResult = calc.calculate(
            listOf(
                BiometricPayload("w1", "WearOS", 3000L, 62.0, 84.0, 35.6, 2.2),
                BiometricPayload("w1", "WearOS", 4000L, 63.0, 86.0, 35.7, 2.3),
            ),
        )

        val beforeNarrative = AuraReadingInterpreter.interpret(beforeResult)
        val afterNarrative = AuraReadingInterpreter.interpret(afterResult)
        assertTrue(afterNarrative.archetypeTitle.isNotBlank())
        assertTrue(afterNarrative.energeticEssenceTag.isNotBlank())
        assertTrue(afterNarrative.innerLayerSummary.contains("Inner Core"))
        assertTrue(afterNarrative.ascendantTitle.contains("Rising"))
        assertTrue(afterNarrative.descendantTitle.contains("Polarity"))
        assertTrue(afterNarrative.personalityInsight.isNotBlank())
        assertTrue(afterNarrative.loveInsight.isNotBlank())
        assertTrue(afterNarrative.careerInsight.isNotBlank())
        assertTrue(afterNarrative.dailyMantra.isNotBlank())

        val zodiacNarrative = AuraReadingInterpreter.interpret(
            result = afterResult,
            birthProfile = com.example.natureaura.model.BirthChartProfile(
                birthMonth = 10,
                birthDay = 14,
                birthHour24 = 8,
            ),
        )
        assertTrue(zodiacNarrative.ascendantTitle.contains("Scorpio Rising"))
        assertTrue(zodiacNarrative.descendantTitle.contains("Taurus 7th House"))

        val delta = AuraReadingInterpreter.computeDelta(
            before = com.example.natureaura.engine.AuraReadingRecord(
                id = 1L,
                timestampMs = 1000L,
                photoPath = null,
                sourcePlatform = "WearOS",
                result = beforeResult,
                narrative = beforeNarrative,
            ),
            after = com.example.natureaura.engine.AuraReadingRecord(
                id = 2L,
                timestampMs = 3000L,
                photoPath = null,
                sourcePlatform = "WearOS",
                parentReadingId = 1L,
                result = afterResult,
                narrative = afterNarrative,
            ),
        )
        assertTrue(delta.harmonyDeltaPercent > 0)
        assertTrue(delta.hrvDeltaMs > 0)

        val signature = AuraReadingInterpreter.computeColorSignature(afterResult)
        assertEquals(7, signature.size)
        assertEquals(100, signature.sumOf { it.percentage })
        assertTrue(signature.first().percentage >= signature.last().percentage)
    }

    @Test
    fun `offline repository persists reading, baseline calibration, and composited photograph`() = runTest {
        val calc = AuraCalculator()
        val result = calc.calculate(
            listOf(
                BiometricPayload("w1", "WearOS", 1730000000000L, 64.0, 78.0, 35.2, 2.8),
            ),
        )

        val saved = repository.saveCaptureAndReading(
            result = result,
            sourcePlatform = "WearOS",
            imageCapture = null,
        )

        val storedList = repository.allReadings.first()
        assertEquals(1, storedList.size)
        assertEquals(saved.id, storedList.first().id)
        assertNotNull(saved.photoPath)
        assertTrue(File(saved.photoPath!!).exists())

        val baseline = repository.getBaselineOnce()
        assertNotNull(baseline)
        assertEquals(1, baseline!!.scanCount)

        repository.deleteReading(saved)
        val afterDelete = repository.allReadings.first()
        assertTrue(afterDelete.isEmpty())
    }
}
