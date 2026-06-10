package com.elg.swiftsplit.domain.model

data class Run(
    val id: RunId = RunId.generate(),
    val gameInfo: GameInfo,
    val segments: List<Segment> = emptyList(),
    val attemptCount: Int = 0,
    val attemptHistory: List<Attempt> = emptyList(),
    val offset: TimeSpan = TimeSpan.ZERO,
    val layoutPath: String? = null,
    val autoSplitterSettings: String? = null
) {
    val personalBest: SplitTime?
        get() = segments.lastOrNull()?.splitTimes?.get(ComparisonName.PERSONAL_BEST)

    fun withCompletedAttempt(
        attemptId: Int,
        startedAt: String?,
        endedAt: String?,
        pauseTime: TimeSpan,
        timingMethod: TimingMethod,
        finalTime: TimeSpan?,
        splitTimes: List<TimeSpan?>
    ): Run {
        val attempt = Attempt(
            id = attemptId,
            startedAt = startedAt,
            endedAt = endedAt,
            realTime = if (timingMethod == TimingMethod.REAL_TIME) finalTime else null,
            gameTime = if (timingMethod == TimingMethod.GAME_TIME) finalTime else null,
            pauseTime = pauseTime
        )

        val isCompleted = finalTime != null
        val updatedSegments = segments.mapIndexed { idx, segment ->
            val splitVal = splitTimes.getOrNull(idx)
            val segmentVal = com.elg.swiftsplit.domain.service.SplitTimeCalculator.getSegmentTime(splitTimes, idx)

            val newSplitTimes = segment.splitTimes.toMutableMap()
            
            if (isCompleted && splitVal != null && finalTime != null) {
                val pbTime = personalBest?.getTime(timingMethod)
                if (pbTime == null || finalTime < pbTime) {
                    newSplitTimes[ComparisonName.PERSONAL_BEST] = SplitTime(
                        realTime = if (timingMethod == TimingMethod.REAL_TIME) splitVal else segment.splitTimes[ComparisonName.PERSONAL_BEST]?.realTime,
                        gameTime = if (timingMethod == TimingMethod.GAME_TIME) splitVal else segment.splitTimes[ComparisonName.PERSONAL_BEST]?.gameTime
                    )
                }
            }

            if (segmentVal != null) {
                val newHistoryEntry = SegmentHistoryEntry(
                    attemptId = attemptId,
                    time = SplitTime(
                        realTime = if (timingMethod == TimingMethod.REAL_TIME) segmentVal else null,
                        gameTime = if (timingMethod == TimingMethod.GAME_TIME) segmentVal else null
                    )
                )

                val bestTimeSpan = segment.bestSegmentTime?.getTime(timingMethod)
                val newBest = if (bestTimeSpan == null || segmentVal < bestTimeSpan) {
                    SplitTime(
                        realTime = if (timingMethod == TimingMethod.REAL_TIME) segmentVal else segment.bestSegmentTime?.realTime,
                        gameTime = if (timingMethod == TimingMethod.GAME_TIME) segmentVal else segment.bestSegmentTime?.gameTime
                    )
                } else {
                    segment.bestSegmentTime
                }

                segment.copy(
                    bestSegmentTime = newBest,
                    segmentHistory = segment.segmentHistory + newHistoryEntry,
                    splitTimes = newSplitTimes
                )
            } else {
                segment.copy(splitTimes = newSplitTimes)
            }
        }

        return this.copy(
            attemptCount = attemptId,
            attemptHistory = attemptHistory + attempt,
            segments = updatedSegments
        )
    }
}
