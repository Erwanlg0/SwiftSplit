package com.elg.swiftsplit.ui.screen.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.ObserveTimerLayoutPreferencesUseCase
import com.elg.swiftsplit.application.port.input.UpdateTimerLayoutPreferencesUseCase
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LayoutEditorViewModel @Inject constructor(
    observeTimerLayoutPreferencesUseCase: ObserveTimerLayoutPreferencesUseCase,
    private val updateTimerLayoutPreferencesUseCase: UpdateTimerLayoutPreferencesUseCase
) : ViewModel() {

    val layoutPreferences = observeTimerLayoutPreferencesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    fun setShowLeadingZeros(enabled: Boolean) {
        update { it.copy(timeFormat = it.timeFormat.copy(showLeadingZeros = enabled)) }
    }

    fun setFormatPattern(pattern: com.elg.swiftsplit.domain.model.TimeFormatPattern) {
        update { it.copy(timeFormat = it.timeFormat.copy(pattern = pattern)) }
    }

    fun setDecimalPlaces(places: Int) {
        update { it.copy(timeFormat = it.timeFormat.copy(decimalPlaces = places.coerceIn(1, 3))) }
    }

    fun setShowFraction(enabled: Boolean) {
        update { it.copy(timeFormat = it.timeFormat.copy(showFraction = enabled)) }
    }

    fun setColorMode(mode: TimerColorMode) {
        update { it.copy(colorMode = mode) }
    }

    fun setRunningStateColor(color: com.elg.swiftsplit.domain.model.StateColorPreset) {
        update { it.copy(stateColorRunning = color) }
    }

    fun setPausedStateColor(color: com.elg.swiftsplit.domain.model.StateColorPreset) {
        update { it.copy(stateColorPaused = color) }
    }

    fun setFinishedStateColor(color: com.elg.swiftsplit.domain.model.StateColorPreset) {
        update { it.copy(stateColorFinished = color) }
    }

    fun setShowSplits(enabled: Boolean) {
        update { it.copy(showSplits = enabled) }
    }

    fun setFullscreenOrientation(orientation: com.elg.swiftsplit.domain.model.FullscreenOrientationPreset) {
        update { it.copy(fullscreenOrientation = orientation) }
    }

    fun setShowSplitsFraction(enabled: Boolean) {
        update { it.copy(showSplitsFraction = enabled) }
    }

    fun setSplitsDecimalPlaces(places: Int) {
        update { it.copy(splitsDecimalPlaces = places.coerceIn(0, 3)) }
    }

    fun setSplitApproachThresholdSeconds(seconds: Int) {
        update { it.copy(splitApproachThresholdSeconds = seconds.coerceIn(0, 120)) }
    }

    fun setShowUndoButton(enabled: Boolean) {
        update { it.copy(showUndoButton = enabled) }
    }

    fun setShowSkipButton(enabled: Boolean) {
        update { it.copy(showSkipButton = enabled) }
    }

    fun setShowPauseButton(enabled: Boolean) {
        update { it.copy(showPauseButton = enabled) }
    }

    fun setEnableVibration(enabled: Boolean) {
        update { it.copy(enableVibration = enabled) }
    }

    fun setShowSumOfBest(enabled: Boolean) {
        update { it.copy(showSumOfBest = enabled) }
    }

    fun setShowSegmentDurations(enabled: Boolean) {
        update { it.copy(showSegmentDurations = enabled) }
    }

    fun setIsMinimalistMode(enabled: Boolean) {
        update { it.copy(isMinimalistMode = enabled) }
    }

    fun setUseSubsplits(enabled: Boolean) {
        update { it.copy(useSubsplits = enabled) }
    }

    fun setShowPossibleTimeSave(enabled: Boolean) {
        update { it.copy(showPossibleTimeSave = enabled) }
    }

    fun setBackgroundGradientEnabled(enabled: Boolean) {
        update { it.copy(backgroundGradientEnabled = enabled) }
    }

    fun setBackgroundGradientStart(color: String) {
        update { it.copy(backgroundGradientStart = color) }
    }

    fun setBackgroundGradientEnd(color: String) {
        update { it.copy(backgroundGradientEnd = color) }
    }

    fun setTimerLocked(enabled: Boolean) {
        update { it.copy(timerLocked = enabled) }
    }

    fun setConfirmReset(enabled: Boolean) {
        update { it.copy(confirmReset = enabled) }
    }

    fun setSplitDebounceMs(ms: Long) {
        update { it.copy(splitDebounceMs = ms) }
    }

    fun setTimerTextShadow(enabled: Boolean) {
        update { it.copy(timerTextShadow = enabled) }
    }

    fun setSegmentOpacity(opacity: Float) {
        update { it.copy(segmentOpacity = opacity.coerceIn(0f, 1f)) }
    }

    fun setAutoLockInFullscreen(enabled: Boolean) {
        update { it.copy(autoLockInFullscreen = enabled) }
    }

    private fun update(transform: (TimerLayoutPreferences) -> TimerLayoutPreferences) {
        viewModelScope.launch {
            val current = layoutPreferences.value
            updateTimerLayoutPreferencesUseCase(transform(current))
        }
    }
}
