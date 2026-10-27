package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.PressureUnit
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import com.example.domain.model.WindSpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aeropulse_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SEA_LEVEL_REF_HPA = doublePreferencesKey("sea_level_ref_hpa")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_launch")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val tempUnit = TemperatureUnit.entries.firstOrNull {
            it.name == prefs[Keys.TEMP_UNIT]
        } ?: TemperatureUnit.CELSIUS

        val windUnit = WindSpeedUnit.entries.firstOrNull {
            it.name == prefs[Keys.WIND_UNIT]
        } ?: WindSpeedUnit.KMH

        val pressureUnit = PressureUnit.entries.firstOrNull {
            it.name == prefs[Keys.PRESSURE_UNIT]
        } ?: PressureUnit.HPA

        val themeMode = ThemeMode.entries.firstOrNull {
            it.name == prefs[Keys.THEME_MODE]
        } ?: ThemeMode.DARK

        UserPreferences(
            tempUnit = tempUnit,
            windUnit = windUnit,
            pressureUnit = pressureUnit,
            themeMode = themeMode,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            seaLevelReferenceHpa = prefs[Keys.SEA_LEVEL_REF_HPA] ?: 1013.25,
            autoSyncOnLaunch = prefs[Keys.AUTO_SYNC] ?: true
        )
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { it[Keys.TEMP_UNIT] = unit.name }
    }

    suspend fun setWindSpeedUnit(unit: WindSpeedUnit) {
        context.dataStore.edit { it[Keys.WIND_UNIT] = unit.name }
    }

    suspend fun setPressureUnit(unit: PressureUnit) {
        context.dataStore.edit { it[Keys.PRESSURE_UNIT] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setSeaLevelReferenceHpa(qnhHpa: Double) {
        context.dataStore.edit { it[Keys.SEA_LEVEL_REF_HPA] = qnhHpa.coerceIn(920.0, 1070.0) }
    }

    suspend fun setAutoSyncOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }
}
