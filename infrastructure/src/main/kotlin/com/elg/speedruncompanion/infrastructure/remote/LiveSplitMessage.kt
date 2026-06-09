package com.elg.speedruncompanion.infrastructure.remote

import kotlinx.serialization.Serializable

@Serializable
data class LiveSplitEvent(
    val event: String,
    val payload: String? = null
)

@Serializable
data class LiveSplitResponse(
    val status: String,
    val error: String? = null,
    val payload: String? = null
)
