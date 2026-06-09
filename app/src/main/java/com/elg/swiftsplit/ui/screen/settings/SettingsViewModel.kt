package com.elg.swiftsplit.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPort: SettingsPort
) : ViewModel() {

    val timingMethod = settingsPort.observeTimingMethod().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimingMethod.REAL_TIME
    )

    val comparison = settingsPort.observeComparison().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComparisonName.PERSONAL_BEST
    )

    val saveQuickRuns = settingsPort.observeSaveQuickRuns().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val language = settingsPort.observeLanguage().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "auto"
    )

    val themeMode = settingsPort.observeThemeMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val globalHotkeysEnabled = settingsPort.observeGlobalHotkeysEnabled().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val networkPreferences = settingsPort.observeNetworkPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkPreferences.DEFAULT
    )

    fun setTimingMethod(method: TimingMethod) {
        viewModelScope.launch {
            settingsPort.setTimingMethod(method)
        }
    }

    fun setComparison(comparisonName: ComparisonName) {
        viewModelScope.launch {
            settingsPort.setComparison(comparisonName)
        }
    }

    fun setGlobalHotkeysEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPort.setGlobalHotkeysEnabled(enabled)
        }
    }

    fun setSaveQuickRuns(enabled: Boolean) {
        viewModelScope.launch {
            settingsPort.setSaveQuickRuns(enabled)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsPort.setLanguage(lang)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsPort.setThemeMode(mode)
        }
    }

    fun setPollingDelayMs(delayMs: Long) {
        viewModelScope.launch {
            val current = networkPreferences.value
            settingsPort.setNetworkPreferences(current.copy(pollingDelayMs = delayMs))
        }
    }

    fun setNetworkTimeoutMs(timeoutMs: Long) {
        viewModelScope.launch {
            val current = networkPreferences.value
            settingsPort.setNetworkPreferences(current.copy(networkTimeoutMs = timeoutMs))
        }
    }
}
