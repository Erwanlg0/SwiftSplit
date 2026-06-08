package com.elg.speedruncompanion.infrastructure.parser

import com.elg.speedruncompanion.domain.model.TimeSpan

/**
 * Parses LiveSplit time strings in the format "[d.]HH:MM:SS.NNNNNNN"
 * where days are optional and fractional digits represent 100ns precision.
 *
 * Also formats TimeSpan values back to LiveSplit's canonical format.
 */
internal object LssTimeParser {

    /**
     * Parses a LiveSplit time string to a [TimeSpan].
     *
     * Supported formats:
     * - "HH:MM:SS.NNNNNNN"
     * - "d.HH:MM:SS.NNNNNNN"
     * - "HH:MM:SS" (no fractional)
     * - "-HH:MM:SS.NNNNNNN" (negative)
     *
     * @return parsed [TimeSpan], or null if the string is empty/blank.
     * @throws IllegalArgumentException if the format is invalid.
     */
    fun parse(timeString: String?): TimeSpan? {
        if (timeString.isNullOrBlank()) return null

        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return null

        val negative = trimmed.startsWith("-")
        val working = if (negative) trimmed.substring(1) else trimmed

        // Split into days portion and time portion
        // Format is either "d.HH:MM:SS.NNNNNNN" or "HH:MM:SS.NNNNNNN"
        // Days portion uses a dot separator, but so does the fractional seconds.
        // Days are separated by finding the first dot before the first colon.
        val firstColon = working.indexOf(':')
        val firstDot = working.indexOf('.')

        var days = 0L
        val timePart: String

        if (firstDot != -1 && firstColon != -1 && firstDot < firstColon) {
            // Has days portion: "d.HH:MM:SS.NNNNNNN"
            days = working.substring(0, firstDot).toLongOrNull()
                ?: throw IllegalArgumentException("Invalid days in time: $timeString")
            timePart = working.substring(firstDot + 1)
        } else {
            timePart = working
        }

        // Parse "HH:MM:SS" or "HH:MM:SS.NNNNNNN"
        val colonParts = timePart.split(':')
        if (colonParts.size != 3) {
            throw IllegalArgumentException("Invalid time format (expected HH:MM:SS): $timeString")
        }

        val hours = colonParts[0].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid hours in time: $timeString")
        val minutes = colonParts[1].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid minutes in time: $timeString")

        // Seconds may have fractional part
        val secondsPart = colonParts[2]
        val dotIndex = secondsPart.indexOf('.')

        val wholeSeconds: Long
        val fractionMs: Long

        if (dotIndex == -1) {
            wholeSeconds = secondsPart.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid seconds in time: $timeString")
            fractionMs = 0L
        } else {
            wholeSeconds = secondsPart.substring(0, dotIndex).toLongOrNull()
                ?: throw IllegalArgumentException("Invalid seconds in time: $timeString")
            val fractionStr = secondsPart.substring(dotIndex + 1)
            // LiveSplit uses 7 fractional digits (100ns precision)
            // Pad or truncate to 7 digits, then convert to milliseconds (divide by 10000)
            val paddedFraction = fractionStr.padEnd(7, '0').take(7)
            val hundredNanos = paddedFraction.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid fractional seconds in time: $timeString")
            // 10,000 hundred-nanoseconds = 1 millisecond
            fractionMs = hundredNanos / 10_000L
        }

        val totalMs = days * 86_400_000L +
                hours * 3_600_000L +
                minutes * 60_000L +
                wholeSeconds * 1_000L +
                fractionMs

        return TimeSpan(if (negative) -totalMs else totalMs)
    }

    /**
     * Formats a [TimeSpan] to LiveSplit's canonical time format.
     * Output: "HH:MM:SS.NNNNNNN" or "d.HH:MM:SS.NNNNNNN" when days > 0.
     */
    fun format(timeSpan: TimeSpan?): String? {
        if (timeSpan == null) return null

        val ms = timeSpan.milliseconds
        val negative = ms < 0
        val absMs = kotlin.math.abs(ms)

        val days = absMs / 86_400_000L
        val hours = (absMs % 86_400_000L) / 3_600_000L
        val minutes = (absMs % 3_600_000L) / 60_000L
        val seconds = (absMs % 60_000L) / 1_000L
        val remainingMs = absMs % 1_000L

        // Convert ms to hundred-nanoseconds (7 digits)
        val hundredNanos = remainingMs * 10_000L

        val sb = StringBuilder()
        if (negative) sb.append('-')
        if (days > 0) sb.append("$days.")
        sb.append(hours.toString().padStart(2, '0'))
        sb.append(':')
        sb.append(minutes.toString().padStart(2, '0'))
        sb.append(':')
        sb.append(seconds.toString().padStart(2, '0'))
        sb.append('.')
        sb.append(hundredNanos.toString().padStart(7, '0'))

        return sb.toString()
    }
}
