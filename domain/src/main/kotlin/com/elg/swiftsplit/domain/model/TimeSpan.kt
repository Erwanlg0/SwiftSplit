package com.elg.swiftsplit.domain.model

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

        val pattern = options.pattern
        val showHours = when (pattern) {
            TimeFormatPattern.HH_MM_SS_SS, TimeFormatPattern.HH_MM_SS_S, TimeFormatPattern.HH_MM_SS -> true
            TimeFormatPattern.OPT_HH_MM_SS_SS, TimeFormatPattern.OPT_HH_MM_SS_S, TimeFormatPattern.OPT_HH_MM_SS -> h > 0
            TimeFormatPattern.OPT_HH_OPT_MM_SS_SS, TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> h > 0
            else -> false
        }

        val showMinutes = when (pattern) {
            TimeFormatPattern.HH_MM_SS_SS, TimeFormatPattern.HH_MM_SS_S, TimeFormatPattern.HH_MM_SS -> true
            TimeFormatPattern.OPT_HH_MM_SS_SS, TimeFormatPattern.OPT_HH_MM_SS_S, TimeFormatPattern.OPT_HH_MM_SS -> true
            TimeFormatPattern.OPT_HH_OPT_MM_SS_SS, TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> h > 0 || m > 0
            TimeFormatPattern.MM_SS_SS, TimeFormatPattern.MM_SS_S, TimeFormatPattern.MM_SS -> true
            TimeFormatPattern.OPT_MM_SS_SS, TimeFormatPattern.OPT_MM_SS_S, TimeFormatPattern.OPT_MM_SS -> m > 0
            else -> false
        }

        val padMinutes = when (pattern) {
            TimeFormatPattern.HH_MM_SS_SS, TimeFormatPattern.HH_MM_SS_S, TimeFormatPattern.HH_MM_SS -> true
            TimeFormatPattern.OPT_HH_MM_SS_SS, TimeFormatPattern.OPT_HH_MM_SS_S, TimeFormatPattern.OPT_HH_MM_SS -> h > 0
            TimeFormatPattern.OPT_HH_OPT_MM_SS_SS, TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> h > 0
            TimeFormatPattern.MM_SS_SS, TimeFormatPattern.MM_SS_S, TimeFormatPattern.MM_SS -> true
            TimeFormatPattern.OPT_MM_SS_SS, TimeFormatPattern.OPT_MM_SS_S, TimeFormatPattern.OPT_MM_SS -> false
            else -> false
        }

        val padSeconds = showMinutes

        val decimals = when (pattern) {
            TimeFormatPattern.HH_MM_SS_SS, TimeFormatPattern.OPT_HH_MM_SS_SS, TimeFormatPattern.OPT_HH_OPT_MM_SS_SS,
            TimeFormatPattern.MM_SS_SS, TimeFormatPattern.OPT_MM_SS_SS, TimeFormatPattern.SS_SS -> 2

            TimeFormatPattern.HH_MM_SS_S, TimeFormatPattern.OPT_HH_MM_SS_S, TimeFormatPattern.OPT_HH_OPT_MM_SS_S,
            TimeFormatPattern.MM_SS_S, TimeFormatPattern.OPT_MM_SS_S, TimeFormatPattern.SS_S -> 1

            else -> 0
        }

        val timeString = buildString {
            if (showHours) {
                append(h.toString().padStart(2, '0'))
                append(":")
            }
            if (showMinutes) {
                val minStr = if (padMinutes) m.toString().padStart(2, '0') else m.toString()
                append(minStr)
                append(":")
            }
            val secStr = if (padSeconds) s.toString().padStart(2, '0') else s.toString()
            append(secStr)

            if (decimals > 0) {
                append(".")
                val fraction = when (decimals) {
                    1 -> ms / 100
                    2 -> ms / 10
                    else -> ms
                }
                append(fraction.toString().padStart(decimals, '0'))
            }
        }

        return if (isNegative) "-$timeString" else timeString
    }

    fun formattedWithSign(showMilliseconds: Boolean = true, decimalPlaces: Int = 2): String {
        if (isZero) {
            return if (showMilliseconds && decimalPlaces > 0) {
                "0." + "0".repeat(decimalPlaces)
            } else {
                "0"
            }
        }
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
            if (showMilliseconds && decimalPlaces > 0) {
                append(".")
                val divisor = when (decimalPlaces) {
                    1 -> 100
                    2 -> 10
                    else -> 1
                }
                val fraction = ms / divisor
                append(fraction.toString().padStart(decimalPlaces, '0'))
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

        
        fun fromTimeString(timeStr: String?): TimeSpan? {
            if (timeStr.isNullOrBlank()) return null
            try {
                var cleaned = timeStr.trim()
                val isNeg = cleaned.startsWith("-")
                if (isNeg) {
                    cleaned = cleaned.substring(1)
                }

                
                var days = 0L
                if (cleaned.contains('.') && cleaned.indexOf('.') < cleaned.indexOf(':')) {
                    val dotIdx = cleaned.indexOf('.')
                    days = cleaned.substring(0, dotIdx).toLongOrNull() ?: 0L
                    cleaned = cleaned.substring(dotIdx + 1)
                }

                
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

                
                val fractionVal = fractionPart.take(7).padEnd(7, '0').toLongOrNull() ?: 0L
                
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
