package com.example.natureaura.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.natureaura.model.BirthChartProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.birthChartDataStore by preferencesDataStore(name = "nature_aura_birth_chart")

class BirthChartPreferences(private val context: Context) {

    private companion object {
        val KEY_MONTH = intPreferencesKey("birth_month")
        val KEY_DAY = intPreferencesKey("birth_day")
        val KEY_HOUR = intPreferencesKey("birth_hour24")
    }

    val birthProfileFlow: Flow<BirthChartProfile?> = context.birthChartDataStore.data.map { prefs ->
        val month = prefs[KEY_MONTH]
        val day = prefs[KEY_DAY]
        val hour = prefs[KEY_HOUR]
        if (month != null && day != null && hour != null) {
            BirthChartProfile(month, day, hour)
        } else {
            null
        }
    }

    suspend fun saveProfile(profile: BirthChartProfile) {
        context.birthChartDataStore.edit { prefs ->
            prefs[KEY_MONTH] = profile.birthMonth.coerceIn(1, 12)
            prefs[KEY_DAY] = profile.birthDay.coerceIn(1, 31)
            prefs[KEY_HOUR] = profile.birthHour24.coerceIn(0, 23)
        }
    }

    suspend fun clearProfile() {
        context.birthChartDataStore.edit { prefs ->
            prefs.remove(KEY_MONTH)
            prefs.remove(KEY_DAY)
            prefs.remove(KEY_HOUR)
        }
    }
}
