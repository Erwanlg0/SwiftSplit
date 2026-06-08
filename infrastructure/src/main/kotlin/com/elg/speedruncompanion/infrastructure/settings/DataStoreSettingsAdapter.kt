package com.elg.speedruncompanion.infrastructure.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.domain.model.ComparisonName
import com.elg.speedruncompanion.domain.model.NetworkPreferences
import com.elg.speedruncompanion.domain.model.TimeFormatOptions
import com.elg.speedruncompanion.domain.model.TimerColorMode
import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import com.elg.speedruncompanion.domain.model.TimingMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "speedrun_companion_settings")

@Singleton
class DataStoreSettingsAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsPort {

    private object PreferencesKeys {
        val TIMING_METHOD = stringPreferencesKey("timing_method")
        val COMPARISON_NAME = stringPreferencesKey("comparison_name")
        val GLOBAL_HOTKEYS = booleanPreferencesKey("global_hotkeys_enabled")
        val SAVE_QUICK_RUNS = booleanPreferencesKey("save_quick_runs")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TIMER_SHOW_LEADING_ZEROS = booleanPreferencesKey("timer_show_leading_zeros")
        val TIMER_DECIMAL_PLACES = intPreferencesKey("timer_decimal_places")
        val TIMER_SHOW_FRACTION = booleanPreferencesKey("timer_show_fraction")
        val TIMER_COLOR_MODE = stringPreferencesKey("timer_color_mode")
        val POLLING_DELAY_MS = longPreferencesKey("polling_delay_ms")
        val NETWORK_TIMEOUT_MS = longPreferencesKey("network_timeout_ms")
    }

    override fun observeTimingMethod(): Flow<TimingMethod> {
        return context.dataStore.data.map { preferences ->
            val methodStr = preferences[PreferencesKeys.TIMING_METHOD] ?: TimingMethod.REAL_TIME.name
            try {
                TimingMethod.valueOf(methodStr)
            } catch (e: Exception) {
                TimingMethod.REAL_TIME
            }
        }
    }

    override suspend fun setTimingMethod(method: TimingMethod) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMING_METHOD] = method.name
        }
    }

    override fun observeComparison(): Flow<ComparisonName> {
        return context.dataStore.data.map { preferences ->
            val compStr = preferences[PreferencesKeys.COMPARISON_NAME] ?: ComparisonName.PERSONAL_BEST.name
            ComparisonName(compStr)
        }
    }

    override suspend fun setComparison(comparison: ComparisonName) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPARISON_NAME] = comparison.name
        }
    }

    override fun observeGlobalHotkeysEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.GLOBAL_HOTKEYS] ?: false
        }
    }

    override suspend fun setGlobalHotkeysEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOBAL_HOTKEYS] = enabled
        }
    }

    override fun observeSaveQuickRuns(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SAVE_QUICK_RUNS] ?: true
        }
    }

    override suspend fun setSaveQuickRuns(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_QUICK_RUNS] = enabled
        }
    }

    override fun observeLanguage(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "auto"
        }
    }

    override suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    override fun observeThemeMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: "system"
        }
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    override fun observeTimerLayoutPreferences(): Flow<TimerLayoutPreferences> {
        return context.dataStore.data.map { preferences ->
            val decimalPlaces = preferences[PreferencesKeys.TIMER_DECIMAL_PLACES] ?: 3
            val colorModeStr = preferences[PreferencesKeys.TIMER_COLOR_MODE] ?: TimerColorMode.DELTA.name
            val colorMode = try {
                TimerColorMode.valueOf(colorModeStr)
            } catch (e: Exception) {
                TimerColorMode.DELTA
            }
            TimerLayoutPreferences(
                timeFormat = TimeFormatOptions(
                    showLeadingZeros = preferences[PreferencesKeys.TIMER_SHOW_LEADING_ZEROS] ?: false,
                    decimalPlaces = decimalPlaces.coerceIn(1, 3),
                    showFraction = preferences[PreferencesKeys.TIMER_SHOW_FRACTION] ?: true
                ),
                colorMode = colorMode
            )
        }
    }

    override suspend fun setTimerLayoutPreferences(preferences: TimerLayoutPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TIMER_SHOW_LEADING_ZEROS] = preferences.timeFormat.showLeadingZeros
            prefs[PreferencesKeys.TIMER_DECIMAL_PLACES] = preferences.timeFormat.decimalPlaces
            prefs[PreferencesKeys.TIMER_SHOW_FRACTION] = preferences.timeFormat.showFraction
            prefs[PreferencesKeys.TIMER_COLOR_MODE] = preferences.colorMode.name
        }
    }

    override fun observeNetworkPreferences(): Flow<NetworkPreferences> {
        return context.dataStore.data.map { preferences ->
            NetworkPreferences(
                pollingDelayMs = (preferences[PreferencesKeys.POLLING_DELAY_MS]
                    ?: NetworkPreferences.DEFAULT_POLLING_DELAY_MS)
                    .coerceIn(NetworkPreferences.MIN_POLLING_DELAY_MS, NetworkPreferences.MAX_POLLING_DELAY_MS),
                networkTimeoutMs = (preferences[PreferencesKeys.NETWORK_TIMEOUT_MS]
                    ?: NetworkPreferences.DEFAULT_NETWORK_TIMEOUT_MS)
                    .coerceIn(NetworkPreferences.MIN_NETWORK_TIMEOUT_MS, NetworkPreferences.MAX_NETWORK_TIMEOUT_MS)
            )
        }
    }

    override suspend fun setNetworkPreferences(preferences: NetworkPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.POLLING_DELAY_MS] = preferences.pollingDelayMs
            prefs[PreferencesKeys.NETWORK_TIMEOUT_MS] = preferences.networkTimeoutMs
        }
    }
}
