package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.TimeSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SplitTimeCalculatorTest {

    @Test
    fun `getSegmentTime should return first split for index 0`() {
        val splitTimes = listOf(TimeSpan.fromSeconds(10.0), TimeSpan.fromSeconds(25.0))
        val result = SplitTimeCalculator.getSegmentTime(splitTimes, 0)
        assertEquals(10000L, result?.totalMilliseconds)
    }

    @Test
    fun `getSegmentTime should return difference for later indices`() {
        val splitTimes = listOf(TimeSpan.fromSeconds(10.0), TimeSpan.fromSeconds(25.0))
        val result = SplitTimeCalculator.getSegmentTime(splitTimes, 1)
        assertEquals(15000L, result?.totalMilliseconds)
    }

    @Test
    fun `getSegmentTime should return null if current split is missing`() {
        val splitTimes = listOf(TimeSpan.fromSeconds(10.0), null)
        assertNull(SplitTimeCalculator.getSegmentTime(splitTimes, 1))
    }

    @Test
    fun `getSegmentTime should return null if previous split is missing`() {
        val splitTimes = listOf(null, TimeSpan.fromSeconds(25.0))
        assertNull(SplitTimeCalculator.getSegmentTime(splitTimes, 1))
    }

    @Test
    fun `getDelta should identify AHEAD_GAINING`() {
        // Comparison split is 10s. We are at 8s. Prev delta was 0 (exact).
        // 8 - 10 = -2s. -2s <= 0 -> Ahead Gaining
        val result = SplitTimeCalculator.getDelta(
            currentSplit = TimeSpan.fromSeconds(8.0),
            comparisonSplit = TimeSpan.fromSeconds(10.0),
            previousCurrentSplit = TimeSpan.ZERO,
            previousComparisonSplit = TimeSpan.ZERO
        )
        assertEquals(Delta.Status.AHEAD_GAINING, result?.status)
    }

    @Test
    fun `getDelta should identify AHEAD_LOSING`() {
        // Prev split: Us 4s, Comp 8s (Delta -4s).
        // Current split: Us 12s, Comp 15s (Delta -3s).
        // -3s > -4s -> Ahead Losing
        val result = SplitTimeCalculator.getDelta(
            currentSplit = TimeSpan.fromSeconds(12.0),
            comparisonSplit = TimeSpan.fromSeconds(15.0),
            previousCurrentSplit = TimeSpan.fromSeconds(4.0),
            previousComparisonSplit = TimeSpan.fromSeconds(8.0)
        )
        assertEquals(Delta.Status.AHEAD_LOSING, result?.status)
    }

    @Test
    fun `getDelta should identify BEHIND_LOSING`() {
        // Prev delta: Us 5s, Comp 5s (Delta 0).
        // Current split: Us 15s, Comp 10s (Delta +5s).
        // +5s > 0 -> Behind Losing
        val result = SplitTimeCalculator.getDelta(
            currentSplit = TimeSpan.fromSeconds(15.0),
            comparisonSplit = TimeSpan.fromSeconds(10.0),
            previousCurrentSplit = TimeSpan.fromSeconds(5.0),
            previousComparisonSplit = TimeSpan.fromSeconds(5.0)
        )
        assertEquals(Delta.Status.BEHIND_LOSING, result?.status)
    }

    @Test
    fun `getDelta should identify BEHIND_GAINING`() {
        // Prev delta: Us 10s, Comp 5s (Delta +5s).
        // Current split: Us 13s, Comp 10s (Delta +3s).
        // +3s <= +5s -> Behind Gaining
        val result = SplitTimeCalculator.getDelta(
            currentSplit = TimeSpan.fromSeconds(13.0),
            comparisonSplit = TimeSpan.fromSeconds(10.0),
            previousCurrentSplit = TimeSpan.fromSeconds(10.0),
            previousComparisonSplit = TimeSpan.fromSeconds(5.0)
        )
        assertEquals(Delta.Status.BEHIND_GAINING, result?.status)
    }

    @Test
    fun `getDelta should return null if comparison is missing`() {
        assertNull(SplitTimeCalculator.getDelta(TimeSpan.ZERO, null))
    }
}
