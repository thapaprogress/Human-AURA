package com.example.natureaura.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraReadingDao {

    @Query("SELECT * FROM aura_readings ORDER BY timestampMs DESC")
    fun observeAllReadings(): Flow<List<AuraReadingEntity>>

    @Query("SELECT * FROM aura_readings WHERE id = :id LIMIT 1")
    suspend fun getReadingById(id: Long): AuraReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(entity: AuraReadingEntity): Long

    @Query("DELETE FROM aura_readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)
}

@Dao
interface PersonalBaselineDao {

    @Query("SELECT * FROM personal_baselines WHERE id = 1 LIMIT 1")
    fun observeBaseline(): Flow<PersonalBaselineEntity?>

    @Query("SELECT * FROM personal_baselines WHERE id = 1 LIMIT 1")
    suspend fun getBaselineOnce(): PersonalBaselineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBaseline(baseline: PersonalBaselineEntity)

    @Query("DELETE FROM personal_baselines")
    suspend fun resetBaseline()
}
