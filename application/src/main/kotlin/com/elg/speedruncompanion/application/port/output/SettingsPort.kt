package com.elg.speedruncompanion.application.port.output

import com.elg.speedruncompanion.domain.model.ComparisonName
import com.elg.speedruncompanion.domain.model.NetworkPreferences
import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import com.elg.speedruncompanion.domain.model.TimingMethod
import kotlinx.coroutines.flow.Flow

interface SettingsPort {
    fun observeTimingMethod(): Flow<TimingMethod>
    suspend fun setTimingMethod(method: TimingMethod)
    fun observeComparison(): Flow<ComparisonName>
    suspend fun setComparison(comparison: ComparisonName)
    fun observeGlobalHotkeysEnabled(): Flow<Boolean>
    suspend fun setGlobalHotkeysEnabled(enabled: Boolean)

    fun observeSaveQuickRuns(): Flow<Boolean>
    suspend fun setSaveQuickRuns(enabled: Boolean)
    fun observeLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    fun observeThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)

    fun observeTimerLayoutPreferences(): Flow<TimerLayoutPreferences>
    suspend fun setTimerLayoutPreferences(preferences: TimerLayoutPreferences)

    fun observeNetworkPreferences(): Flow<NetworkPreferences>
    suspend fun setNetworkPreferences(preferences: NetworkPreferences)
}
