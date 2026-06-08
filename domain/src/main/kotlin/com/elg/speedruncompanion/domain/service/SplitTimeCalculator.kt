package com.elg.speedruncompanion.domain.service

import com.elg.speedruncompanion.domain.model.Delta
import com.elg.speedruncompanion.domain.model.TimeSpan

object SplitTimeCalculator {

    fun getSegmentTime(splitTimes: List<TimeSpan?>, segmentIndex: Int): TimeSpan? {
        if (segmentIndex < 0 || segmentIndex >= splitTimes.size) return null
        val currentSplit = splitTimes[segmentIndex] ?: return null
        if (segmentIndex == 0) return currentSplit

        val previousSplit = splitTimes[segmentIndex - 1] ?: return null
        return currentSplit - previousSplit
    }

    /**
     * Calculates the delta and status vs a comparison split time at the current index.
     * Optionally compares to previous delta to see if we are gaining or losing.
     */
    fun getDelta(
        currentSplit: TimeSpan,
        comparisonSplit: TimeSpan?,
        previousCurrentSplit: TimeSpan? = null,
        previousComparisonSplit: TimeSpan? = null,
        isBestSegment: Boolean = false
    ): Delta? {
        if (comparisonSplit == null) return null
        
        if (isBestSegment) {
            return Delta(currentSplit - comparisonSplit, Delta.Status.BEST_SEGMENT)
        }

        val delta = currentSplit - comparisonSplit
        
        val previousDelta = if (previousCurrentSplit != null && previousComparisonSplit != null) {
            previousCurrentSplit - previousComparisonSplit
        } else {
            TimeSpan.ZERO
        }

        val status = if (delta.isNegative) {
            // Ahead
            if (delta <= previousDelta) {
                Delta.Status.AHEAD_GAINING
            } else {
                Delta.Status.AHEAD_LOSING
            }
        } else if (delta.isZero) {
            Delta.Status.EXACT
        } else {
            // Behind
            if (delta <= previousDelta) {
                Delta.Status.BEHIND_GAINING
            } else {
                Delta.Status.BEHIND_LOSING
            }
        }

        return Delta(delta, status)
    }

    fun getSegmentDelta(
        currentSegmentTime: TimeSpan,
        bestSegmentTime: TimeSpan?
    ): Delta? {
        if (bestSegmentTime == null) return null
        val delta = currentSegmentTime - bestSegmentTime
        val status = if (delta.isNegative) {
            Delta.Status.BEST_SEGMENT
        } else if (delta.isZero) {
            Delta.Status.EXACT
        } else {
            Delta.Status.BEHIND_LOSING
        }
        return Delta(delta, status)
    }
}
