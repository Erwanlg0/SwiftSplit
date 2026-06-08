package com.elg.speedruncompanion.domain.model

data class ActiveRun(
    val run: Run,
    val currentSegmentIndex: Int = 0,
    val splitTimes: List<TimeSpan?> = List(run.segments.size) { null },
    val startTime: Long = 0,
    val pauseAccumulator: Long = 0,
    val pauseStart: Long? = null,
    val comparison: ComparisonName = ComparisonName.PERSONAL_BEST,
    val timingMethod: TimingMethod = TimingMethod.REAL_TIME
)
