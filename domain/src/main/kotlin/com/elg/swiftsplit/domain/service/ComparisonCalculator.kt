package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.Segment
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimingMethod

object ComparisonCalculator {

    fun calculateSumOfBest(segments: List<Segment>, method: TimingMethod): TimeSpan? {
        var sum = 0L
        var anyValid = false
        for (segment in segments) {
            val best = segment.bestSegmentTime?.getTime(method)
            if (best != null) {
                sum += best.totalMilliseconds
                anyValid = true
            }
        }
        return if (anyValid) TimeSpan(sum) else null
    }

    fun calculateAverageSegments(segments: List<Segment>, method: TimingMethod): List<TimeSpan?> {
        return segments.map { segment ->
            val validTimes = segment.segmentHistory
                .mapNotNull { it.time.getTime(method)?.totalMilliseconds }
            if (validTimes.isNotEmpty()) {
                TimeSpan(validTimes.average().toLong())
            } else {
                null
            }
        }
    }

    fun calculateBestSplitTimes(segments: List<Segment>, method: TimingMethod): List<TimeSpan?> {
        val result = mutableListOf<TimeSpan?>()
        var accumulated = 0L
        var anyValid = false
        for (segment in segments) {
            val best = segment.bestSegmentTime?.getTime(method)
            if (best != null) {
                accumulated += best.totalMilliseconds
                result.add(TimeSpan(accumulated))
                anyValid = true
            } else {
                result.add(null)
            }
        }
        return if (anyValid) result else List(segments.size) { null }
    }
}
