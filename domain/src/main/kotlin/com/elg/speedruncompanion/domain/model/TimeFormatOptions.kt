package com.elg.speedruncompanion.domain.model

data class TimeFormatOptions(
    val showLeadingZeros: Boolean = false,
    val decimalPlaces: Int = 3,
    val showFraction: Boolean = true
) {
    init {
        require(decimalPlaces in 1..3) { "decimalPlaces must be 1, 2, or 3" }
    }

    companion object {
        val DEFAULT = TimeFormatOptions()
    }
}
