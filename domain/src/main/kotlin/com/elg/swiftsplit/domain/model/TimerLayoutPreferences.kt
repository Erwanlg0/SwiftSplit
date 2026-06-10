package com.elg.swiftsplit.domain.model

data class TimerLayoutPreferences(
    val timeFormat: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    val colorMode: TimerColorMode = TimerColorMode.TIMER_STATE,
    val stateColorRunning: StateColorPreset = StateColorPreset.GREEN,
    val stateColorPaused: StateColorPreset = StateColorPreset.GRAY,
    val stateColorFinished: StateColorPreset = StateColorPreset.BLUE,
    val showSplits: Boolean = true,
    val fullscreenOrientation: FullscreenOrientationPreset = FullscreenOrientationPreset.AUTO,
    val showSplitsFraction: Boolean = true,
    val splitsDecimalPlaces: Int = 2,
    /** Seconds before a comparison split when live delta appears in the active split row (fullscreen). */
    val splitApproachThresholdSeconds: Int = 30
) {
    companion object {
        val DEFAULT = TimerLayoutPreferences()
    }
}

enum class StateColorPreset {
    GREEN,
    BLUE,
    GRAY,
    RED,
    ORANGE,
    GOLD,
    WHITE
}

enum class FullscreenOrientationPreset {
    PORTRAIT,
    LANDSCAPE,
    AUTO;

    fun displayName(): String = when (this) {
        PORTRAIT -> "Vertical (Portrait)"
        LANDSCAPE -> "Horizontal (Landscape)"
        AUTO -> "Automatique (Capteur)"
    }
}
