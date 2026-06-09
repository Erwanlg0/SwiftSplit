package com.elg.swiftsplit.ui.screen.layout

import androidx.compose.ui.graphics.Color
import com.elg.swiftsplit.domain.model.StateColorPreset
import com.elg.swiftsplit.domain.model.TimerDisplayColorToken
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.ui.theme.SpeedrunColorScheme

fun TimerDisplayColorToken.toComposeColor(colors: SpeedrunColorScheme) = when (this) {
    TimerDisplayColorToken.DEFAULT -> colors.timerText
    TimerDisplayColorToken.AHEAD_GAINING -> colors.aheadGaining
    TimerDisplayColorToken.AHEAD_LOSING -> colors.aheadLosing
    TimerDisplayColorToken.BEHIND_LOSING -> colors.behindLosing
    TimerDisplayColorToken.BEHIND_GAINING -> colors.behindGaining
    TimerDisplayColorToken.BEST_SEGMENT -> colors.bestSegment
    TimerDisplayColorToken.STATE_IDLE -> colors.timerTextDim
    TimerDisplayColorToken.STATE_RUNNING -> colors.success
    TimerDisplayColorToken.STATE_PAUSED -> colors.timerTextDim
    TimerDisplayColorToken.STATE_FINISHED -> colors.info
}

fun TimerDisplayColorToken.toComposeColorWithPrefs(
    colors: SpeedrunColorScheme,
    prefs: TimerLayoutPreferences
): Color = when (this) {
    TimerDisplayColorToken.STATE_RUNNING -> prefs.stateColorRunning.toComposeColor(colors)
    TimerDisplayColorToken.STATE_PAUSED -> prefs.stateColorPaused.toComposeColor(colors)
    TimerDisplayColorToken.STATE_FINISHED -> prefs.stateColorFinished.toComposeColor(colors)
    else -> toComposeColor(colors)
}

fun StateColorPreset.toComposeColor(colors: SpeedrunColorScheme): Color = when (this) {
    StateColorPreset.GREEN -> colors.success
    StateColorPreset.BLUE -> colors.info
    StateColorPreset.GRAY -> colors.timerTextDim
    StateColorPreset.RED -> colors.error
    StateColorPreset.ORANGE -> colors.warning
    StateColorPreset.GOLD -> colors.bestSegment
    StateColorPreset.WHITE -> colors.timerText
}

fun StateColorPreset.displayName(): String = when (this) {
    StateColorPreset.GREEN -> "Vert"
    StateColorPreset.BLUE -> "Bleu"
    StateColorPreset.GRAY -> "Gris"
    StateColorPreset.RED -> "Rouge"
    StateColorPreset.ORANGE -> "Orange"
    StateColorPreset.GOLD -> "Or"
    StateColorPreset.WHITE -> "Blanc"
}
