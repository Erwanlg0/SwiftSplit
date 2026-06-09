package com.elg.speedruncompanion.domain.model

enum class TimeFormatPattern {
    HH_MM_SS_SS,         // HH:mm:ss.SS
    HH_MM_SS_S,          // HH:mm:ss.S
    HH_MM_SS,            // HH:mm:ss
    OPT_HH_MM_SS_SS,     // [HH:]mm:ss.SS
    OPT_HH_MM_SS_S,      // [HH:]mm:ss.S
    OPT_HH_MM_SS,        // [HH:]mm:ss
    OPT_HH_OPT_MM_SS_SS, // [HH:][mm:]ss.SS (Default)
    OPT_HH_OPT_MM_SS_S,  // [HH:][mm:]ss.S
    MM_SS_SS,            // mm:ss.SS
    MM_SS_S,             // mm:ss.S
    MM_SS,               // mm:ss
    OPT_MM_SS_SS,        // [mm:]ss.SS
    OPT_MM_SS_S,         // [mm:]ss.S
    OPT_MM_SS,           // [mm:]ss
    SS_SS,               // ss.SS
    SS_S,                // ss.S
    SS                   // ss
}

data class TimeFormatOptions(
    val pattern: TimeFormatPattern = TimeFormatPattern.OPT_HH_OPT_MM_SS_SS,
    // Keep backwards compatibility fields:
    val showLeadingZeros: Boolean = false,
    val decimalPlaces: Int = 3,
    val showFraction: Boolean = true
) {
    companion object {
        val DEFAULT = TimeFormatOptions()
    }
}

