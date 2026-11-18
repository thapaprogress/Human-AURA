package com.example.natureaura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AuraReadingEntity::class, PersonalBaselineEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun auraReadingDao(): AuraReadingDao
    abstract fun personalBaselineDao(): PersonalBaselineDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aura_readings ADD COLUMN parentReadingId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS personal_baselines (
                        id INTEGER NOT NULL PRIMARY KEY,
                        restingHrBpm REAL NOT NULL,
                        baselineHrvMs REAL NOT NULL,
                        baselineSkinTempC REAL NOT NULL,
                        baselineEdaMicroS REAL NOT NULL,
                        scanCount INTEGER NOT NULL,
                        updatedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "nature_aura_offline.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
