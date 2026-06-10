package com.elg.swiftsplit.domain.model

data class SegmentHistoryEntry(
    val attemptId: Int,
    val time: SplitTime
)

data class Segment(
    val name: String,
    val iconData: String? = null,
    val splitTimes: Map<ComparisonName, SplitTime> = emptyMap(),
    val bestSegmentTime: SplitTime? = null,
    val segmentHistory: List<SegmentHistoryEntry> = emptyList()
) {
    val isSubsplit: Boolean get() = name.startsWith("-")
    val cleanedName: String get() = if (isSubsplit) name.substring(1).trim() else name
}
