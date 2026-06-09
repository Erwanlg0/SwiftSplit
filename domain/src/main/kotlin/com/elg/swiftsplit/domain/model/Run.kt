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
}
