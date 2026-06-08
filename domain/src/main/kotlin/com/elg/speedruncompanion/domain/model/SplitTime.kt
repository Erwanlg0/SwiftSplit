package com.elg.speedruncompanion.domain.model

data class SplitTime(
    val realTime: TimeSpan? = null,
    val gameTime: TimeSpan? = null
) {
    fun getTime(method: TimingMethod): TimeSpan? {
        return when (method) {
            TimingMethod.REAL_TIME -> realTime
            TimingMethod.GAME_TIME -> gameTime
        }
    }
}
