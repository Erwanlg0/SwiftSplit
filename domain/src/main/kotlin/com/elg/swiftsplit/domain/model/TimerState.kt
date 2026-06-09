package com.elg.swiftsplit.domain.model

sealed interface TimerState {
    data object Idle : TimerState
    
    data class Running(
        val startTime: Long,
        val pauseAccumulator: Long,
        val currentSegmentIndex: Int,
        val splitTimes: List<TimeSpan?>,
        val comparison: ComparisonName
    ) : TimerState
    
    data class Paused(
        val elapsedTime: TimeSpan,
        val currentSegmentIndex: Int,
        val splitTimes: List<TimeSpan?>,
        val comparison: ComparisonName
    ) : TimerState
    
    data class Finished(
        val finalTime: TimeSpan,
        val splitTimes: List<TimeSpan?>,
        val comparison: ComparisonName
    ) : TimerState
}
