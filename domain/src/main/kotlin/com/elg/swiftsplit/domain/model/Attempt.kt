package com.elg.swiftsplit.domain.model

data class Attempt(
    val id: Int,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val isStartedSynced: Boolean = true,
    val isEndedSynced: Boolean = true,
    val realTime: TimeSpan? = null,
    val gameTime: TimeSpan? = null,
    val pauseTime: TimeSpan? = null
)
