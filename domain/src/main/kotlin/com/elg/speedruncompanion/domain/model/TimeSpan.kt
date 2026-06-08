package com.elg.speedruncompanion.domain.model

import kotlin.math.abs

@JvmInline
value class TimeSpan(val totalMilliseconds: Long) : Comparable<TimeSpan> {

    val hours: Int get() = (totalMilliseconds / 3600000).toInt()
    val minutes: Int get() = ((totalMilliseconds % 3600000) / 60000).toInt()
    val seconds: Int get() = ((totalMilliseconds % 60000) / 1000).toInt()
    val milliseconds: Int get() = (totalMilliseconds % 1000).toInt()

    val isNegative: Boolean get() = totalMilliseconds < 0
    val isZero: Boolean get() = totalMilliseconds == 0L

    operator fun plus(other: TimeSpan): TimeSpan = TimeSpan(this.totalMilliseconds + other.totalMilliseconds)
    operator fun minus(other: TimeSpan): TimeSpan = TimeSpan(this.totalMilliseconds - other.totalMilliseconds)
    operator fun unaryMinus(): TimeSpan = TimeSpan(-this.totalMilliseconds)

    override fun compareTo(other: TimeSpan): Int = this.totalMilliseconds.compareTo(other.totalMilliseconds)

    fun formatted(showMilliseconds: Boolean = true): String {
        val options = if (showMilliseconds) {
            TimeFormatOptions.DEFAULT
        } else {
            TimeFormatOptions(showFraction = false)
        }
        return formatted(options)
    }

    fun formatted(options: TimeFormatOptions): String {
        val absoluteMs = abs(totalMilliseconds)
        val h = absoluteMs / 3600000
        val m = (absoluteMs % 3600000) / 60000
        val s = (absoluteMs % 60000) / 1000
        val ms = absoluteMs % 1000

        val timeString = buildString {
            if (options.showLeadingZeros) {
                append(h.toString().padStart(2, '0'))
                append(":")
                append(m.toString().padStart(2, '0'))
                append(":")
                append(s.toString().padStart(2, '0'))
            } else {
                if (h > 0) {
                    append(h)
                    append(":")
                    if (m < 10) append("0")
                }
                append(m)
                append(":")
                if (s < 10) append("0")
                append(s)
            }
            if (options.showFraction && options.decimalPlaces > 0) {
                append(".")
                val fraction = when (options.decimalPlaces) {
                    1 -> ms / 100
                    2 -> ms / 10
                    else -> ms
                }
                append(fraction.toString().padStart(options.decimalPlaces, '0'))
            }
        }
        return if (isNegative) "-$timeString" else timeString
    }

    fun formattedWithSign(showMilliseconds: Boolean = true): String {
        if (isZero) return "0.00"
        val sign = if (isNegative) "-" else "+"
        val absoluteMs = abs(totalMilliseconds)
        val h = absoluteMs / 3600000
        val m = (absoluteMs % 3600000) / 60000
        val s = (absoluteMs % 60000) / 1000
        val ms = absoluteMs % 1000

        val timeString = buildString {
            if (h > 0) {
                append(h)
                append(":")
                if (m < 10) append("0")
                append(m)
                append(":")
            } else if (m > 0) {
                append(m)
                append(":")
            }
            if (h > 0 || m > 0) {
                if (s < 10) append("0")
            }
            append(s)
            if (showMilliseconds) {
                append(".")
                // Only show 2 decimal digits for split deltas
                val hundredths = ms / 10
                if (hundredths < 10) append("0")
                append(hundredths)
            }
        }
        return "$sign$timeString"
    }

    companion object {
        val ZERO = TimeSpan(0)

        fun fromMilliseconds(ms: Long): TimeSpan = TimeSpan(ms)
        fun fromSeconds(seconds: Double): TimeSpan = TimeSpan((seconds * 1000).toLong())
        fun fromMinutes(minutes: Double): TimeSpan = TimeSpan((minutes * 60000).toLong())
        fun fromHours(hours: Double): TimeSpan = TimeSpan((hours * 3600000).toLong())

        /**
         * Parses LiveSplit TimeSpan format which can be:
         * - HH:MM:SS
         * - HH:MM:SS.fraction
         * - d.HH:MM:SS.fraction
         * - -HH:MM:SS.fraction
         * Returns null if parsing fails.
         */
        fun fromTimeString(timeStr: String?): TimeSpan? {
            if (timeStr.isNullOrBlank()) return null
            try {
                var cleaned = timeStr.trim()
                val isNeg = cleaned.startsWith("-")
                if (isNeg) {
                    cleaned = cleaned.substring(1)
                }

                // Split day if present
                var days = 0L
                if (cleaned.contains('.') && cleaned.indexOf('.') < cleaned.indexOf(':')) {
                    val dotIdx = cleaned.indexOf('.')
                    days = cleaned.substring(0, dotIdx).toLongOrNull() ?: 0L
                    cleaned = cleaned.substring(dotIdx + 1)
                }

                // Split time and fraction
                val parts = cleaned.split('.')
                val timePart = parts[0]
                val fractionPart = if (parts.size > 1) parts[1] else "0"

                val timeTokens = timePart.split(':')
                if (timeTokens.size < 2) return null

                val hours: Long
                val minutes: Long
                val seconds: Long

                if (timeTokens.size == 2) {
                    hours = 0
                    minutes = timeTokens[0].toLong()
                    seconds = timeTokens[1].toLong()
                } else {
                    hours = timeTokens[0].toLong()
                    minutes = timeTokens[1].toLong()
                    seconds = timeTokens[2].toLong()
                }

                // Fraction is up to 7 digits for 100ns precision in .lss, convert to ms
                val fractionVal = fractionPart.take(7).padEnd(7, '0').toLongOrNull() ?: 0L
                // 100ns units: 1 ms = 10,000 units of 100ns
                val msFromFraction = fractionVal / 10000L

                val totalMs = (days * 86400000L) +
                        (hours * 3600000L) +
                        (minutes * 60000L) +
                        (seconds * 1000L) +
                        msFromFraction

                return TimeSpan(if (isNeg) -totalMs else totalMs)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
