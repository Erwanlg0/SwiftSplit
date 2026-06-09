package com.elg.swiftsplit.infrastructure.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "swiftsplit_settings")

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
        val TIMER_FORMAT_PATTERN = stringPreferencesKey("timer_format_pattern")
        val TIMER_SHOW_SPLITS = booleanPreferencesKey("timer_show_splits")
        val TIMER_FULLSCREEN_ORIENTATION = stringPreferencesKey("timer_fullscreen_orientation")
        val TIMER_SHOW_SPLITS_FRACTION = booleanPreferencesKey("timer_show_splits_fraction")
        val TIMER_SPLITS_DECIMAL_PLACES = intPreferencesKey("timer_splits_decimal_places")
        val POLLING_DELAY_MS = longPreferencesKey("polling_delay_ms")
        val NETWORK_TIMEOUT_MS = longPreferencesKey("network_timeout_ms")
        val REMOTE_HOST = stringPreferencesKey("remote_host")
        val REMOTE_PORT = stringPreferencesKey("remote_port")
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
            val name = preferences[PreferencesKeys.COMPARISON_NAME] ?: "Personal Best"
            ComparisonName(name)
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
            preferences[PreferencesKeys.SAVE_QUICK_RUNS] ?: false
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
            val patternStr = preferences[PreferencesKeys.TIMER_FORMAT_PATTERN] ?: com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_SS.name
            val pattern = try {
                com.elg.swiftsplit.domain.model.TimeFormatPattern.valueOf(patternStr)
            } catch (e: Exception) {
                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_SS
            }
            TimerLayoutPreferences(
                timeFormat = TimeFormatOptions(
                    pattern = pattern,
                    showLeadingZeros = preferences[PreferencesKeys.TIMER_SHOW_LEADING_ZEROS] ?: false,
                    decimalPlaces = decimalPlaces.coerceIn(1, 3),
                    showFraction = preferences[PreferencesKeys.TIMER_SHOW_FRACTION] ?: true
                ),
                colorMode = colorMode,
                stateColorRunning = try {
                    com.elg.swiftsplit.domain.model.StateColorPreset.valueOf(preferences[stringPreferencesKey("state_color_running")] ?: "GREEN")
                } catch (e: Exception) { com.elg.swiftsplit.domain.model.StateColorPreset.GREEN },
                stateColorPaused = try {
                    com.elg.swiftsplit.domain.model.StateColorPreset.valueOf(preferences[stringPreferencesKey("state_color_paused")] ?: "GRAY")
                } catch (e: Exception) { com.elg.swiftsplit.domain.model.StateColorPreset.GRAY },
                stateColorFinished = try {
                    com.elg.swiftsplit.domain.model.StateColorPreset.valueOf(preferences[stringPreferencesKey("state_color_finished")] ?: "BLUE")
                } catch (e: Exception) { com.elg.swiftsplit.domain.model.StateColorPreset.BLUE },
                showSplits = preferences[PreferencesKeys.TIMER_SHOW_SPLITS] ?: true,
                fullscreenOrientation = try {
                    com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.valueOf(
                        preferences[PreferencesKeys.TIMER_FULLSCREEN_ORIENTATION]
                            ?: com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.AUTO.name
                    )
                } catch (e: Exception) {
                    com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.AUTO
                },
                showSplitsFraction = preferences[PreferencesKeys.TIMER_SHOW_SPLITS_FRACTION] ?: true,
                splitsDecimalPlaces = (preferences[PreferencesKeys.TIMER_SPLITS_DECIMAL_PLACES] ?: 2).coerceIn(0, 3)
            )
        }
    }

    override suspend fun setTimerLayoutPreferences(preferences: TimerLayoutPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TIMER_SHOW_LEADING_ZEROS] = preferences.timeFormat.showLeadingZeros
            prefs[PreferencesKeys.TIMER_DECIMAL_PLACES] = preferences.timeFormat.decimalPlaces
            prefs[PreferencesKeys.TIMER_SHOW_FRACTION] = preferences.timeFormat.showFraction
            prefs[PreferencesKeys.TIMER_COLOR_MODE] = preferences.colorMode.name
            prefs[PreferencesKeys.TIMER_FORMAT_PATTERN] = preferences.timeFormat.pattern.name
            prefs[stringPreferencesKey("state_color_running")] = preferences.stateColorRunning.name
            prefs[stringPreferencesKey("state_color_paused")] = preferences.stateColorPaused.name
            prefs[stringPreferencesKey("state_color_finished")] = preferences.stateColorFinished.name
            prefs[PreferencesKeys.TIMER_SHOW_SPLITS] = preferences.showSplits
            prefs[PreferencesKeys.TIMER_FULLSCREEN_ORIENTATION] = preferences.fullscreenOrientation.name
            prefs[PreferencesKeys.TIMER_SHOW_SPLITS_FRACTION] = preferences.showSplitsFraction
            prefs[PreferencesKeys.TIMER_SPLITS_DECIMAL_PLACES] = preferences.splitsDecimalPlaces
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

    override fun observeRemoteHost(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.REMOTE_HOST] ?: "192.168.1.10"
        }
    }

    override suspend fun setRemoteHost(host: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMOTE_HOST] = host
        }
    }

    override fun observeRemotePort(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.REMOTE_PORT] ?: "16834"
        }
    }

    override suspend fun setRemotePort(port: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMOTE_PORT] = port
        }
    }
}
