package com.elg.swiftsplit.application.port.output

interface HapticFeedbackPort {
    fun vibrate(durationMs: Long)
}
