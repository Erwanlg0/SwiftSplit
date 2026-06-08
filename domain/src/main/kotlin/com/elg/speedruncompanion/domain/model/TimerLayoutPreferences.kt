package com.elg.speedruncompanion.domain.model

data class TimerLayoutPreferences(
    val timeFormat: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    val colorMode: TimerColorMode = TimerColorMode.DELTA
) {
    companion object {
        val DEFAULT = TimerLayoutPreferences()
    }
}
