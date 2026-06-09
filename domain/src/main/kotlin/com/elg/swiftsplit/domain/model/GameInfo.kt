package com.elg.swiftsplit.domain.model

data class GameInfo(
    val gameName: String,
    val categoryName: String,
    val platform: String = "",
    val region: String = "",
    val usesEmulator: Boolean = false,
    val variables: Map<String, String> = emptyMap()
)
