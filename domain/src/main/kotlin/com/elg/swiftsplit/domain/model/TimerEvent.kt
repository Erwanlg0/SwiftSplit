package com.elg.swiftsplit.domain.model

sealed interface TimerEvent {
    data object Started : TimerEvent
    data class Split(val segmentIndex: Int, val splitTime: TimeSpan) : TimerEvent
    data class Skipped(val segmentIndex: Int) : TimerEvent
    data class Undone(val segmentIndex: Int) : TimerEvent
    data class Reset(val saveAttempt: Boolean) : TimerEvent
    data class Finished(val finalTime: TimeSpan) : TimerEvent
    data object Paused : TimerEvent
    data object Resumed : TimerEvent
}
