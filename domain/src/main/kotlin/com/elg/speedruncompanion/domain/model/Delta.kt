package com.elg.speedruncompanion.domain.model

data class Delta(
    val time: TimeSpan,
    val status: Status
) {
    enum class Status {
        AHEAD_GAINING,
        AHEAD_LOSING,
        BEHIND_LOSING,
        BEHIND_GAINING,
        BEST_SEGMENT,
        EXACT
    }
}
