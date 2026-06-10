package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComparisonCalculatorTest {

    @Test
    fun `calculateSumOfBest should sum all best segment times`() {
        val segments = listOf(
            Segment("S1", bestSegmentTime = SplitTime(realTime = TimeSpan.fromSeconds(10.0))),
            Segment("S2", bestSegmentTime = SplitTime(realTime = TimeSpan.fromSeconds(15.0)))
        )
        val result = ComparisonCalculator.calculateSumOfBest(segments, TimingMethod.REAL_TIME)
        assertEquals(25000L, result?.totalMilliseconds)
    }

    @Test
    fun `calculateSumOfBest should return null if no best segments exist`() {
        val segments = listOf(Segment("S1"), Segment("S2"))
        val result = ComparisonCalculator.calculateSumOfBest(segments, TimingMethod.REAL_TIME)
        assertNull(result)
    }

    @Test
    fun `calculateAverageSegments should calculate mean correctly`() {
        val segments = listOf(
            Segment("S1", segmentHistory = listOf(
                SegmentHistoryEntry(1, SplitTime(realTime = TimeSpan.fromSeconds(10.0))),
                SegmentHistoryEntry(2, SplitTime(realTime = TimeSpan.fromSeconds(20.0)))
            ))
        )
        val result = ComparisonCalculator.calculateAverageSegments(segments, TimingMethod.REAL_TIME)
        assertEquals(15000L, result[0]?.totalMilliseconds)
    }

    @Test
    fun `calculateBestSplitTimes should accumulate best segments`() {
        val segments = listOf(
            Segment("S1", bestSegmentTime = SplitTime(realTime = TimeSpan.fromSeconds(10.0))),
            Segment("S2", bestSegmentTime = SplitTime(realTime = TimeSpan.fromSeconds(15.0)))
        )
        val result = ComparisonCalculator.calculateBestSplitTimes(segments, TimingMethod.REAL_TIME)
        assertEquals(10000L, result[0]?.totalMilliseconds)
        assertEquals(25000L, result[1]?.totalMilliseconds)
    }
}
