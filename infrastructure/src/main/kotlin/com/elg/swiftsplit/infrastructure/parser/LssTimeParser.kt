package com.elg.swiftsplit.infrastructure.parser

import com.elg.swiftsplit.domain.model.TimeSpan


internal object LssTimeParser {

    
    fun parse(timeString: String?): TimeSpan? {
        if (timeString.isNullOrBlank()) return null

        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return null

        val negative = trimmed.startsWith("-")
        val working = if (negative) trimmed.substring(1) else trimmed

        
        
        
        
        val firstColon = working.indexOf(':')
        val firstDot = working.indexOf('.')

        var days = 0L
        val timePart: String

        if (firstDot != -1 && firstColon != -1 && firstDot < firstColon) {
            
            days = working.substring(0, firstDot).toLongOrNull()
                ?: throw IllegalArgumentException("Invalid days in time: $timeString")
            timePart = working.substring(firstDot + 1)
        } else {
            timePart = working
        }

        
        val colonParts = timePart.split(':')
        if (colonParts.size != 3) {
            throw IllegalArgumentException("Invalid time format (expected HH:MM:SS): $timeString")
        }

        val hours = colonParts[0].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid hours in time: $timeString")
        val minutes = colonParts[1].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid minutes in time: $timeString")

        
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
            
            
            val paddedFraction = fractionStr.padEnd(7, '0').take(7)
            val hundredNanos = paddedFraction.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid fractional seconds in time: $timeString")
            
            fractionMs = hundredNanos / 10_000L
        }

        val totalMs = days * 86_400_000L +
                hours * 3_600_000L +
                minutes * 60_000L +
                wholeSeconds * 1_000L +
                fractionMs

        return TimeSpan(if (negative) -totalMs else totalMs)
    }

    
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
