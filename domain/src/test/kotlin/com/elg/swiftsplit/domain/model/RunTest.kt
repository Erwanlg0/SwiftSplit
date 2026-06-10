package com.elg.swiftsplit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunTest {

    @Test
    fun `withCompletedAttempt should increment attempt count and add attempt to history`() {
        val run = Run(
            gameInfo = GameInfo("Game", "Category"),
            segments = listOf(Segment("S1")),
            attemptCount = 1
        )

        val updated = run.withCompletedAttempt(
            attemptId = 2,
            startedAt = "01/01/2024 10:00:00",
            endedAt = "01/01/2024 10:05:00",
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = TimeSpan.fromSeconds(300.0),
            splitTimes = listOf(TimeSpan.fromSeconds(300.0))
        )

        assertEquals(2, updated.attemptCount)
        assertEquals(1, updated.attemptHistory.size)
        assertEquals(2, updated.attemptHistory[0].id)
        assertEquals(300000L, updated.attemptHistory[0].realTime?.totalMilliseconds)
    }

    @Test
    fun `withCompletedAttempt should update personal best if new time is better`() {
        val run = Run(
            gameInfo = GameInfo("Game", "Category"),
            segments = listOf(
                Segment(
                    name = "S1",
                    splitTimes = mapOf(
                        ComparisonName.PERSONAL_BEST to SplitTime(realTime = TimeSpan.fromSeconds(400.0))
                    )
                )
            )
        )

        val updated = run.withCompletedAttempt(
            attemptId = 1,
            startedAt = null,
            endedAt = null,
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = TimeSpan.fromSeconds(300.0),
            splitTimes = listOf(TimeSpan.fromSeconds(300.0))
        )

        val newPb = updated.segments[0].splitTimes[ComparisonName.PERSONAL_BEST]?.realTime
        assertEquals(300000L, newPb?.totalMilliseconds)
    }

    @Test
    fun `withCompletedAttempt should NOT update personal best if new time is worse`() {
        val oldPb = TimeSpan.fromSeconds(300.0)
        val run = Run(
            gameInfo = GameInfo("Game", "Category"),
            segments = listOf(
                Segment(
                    name = "S1",
                    splitTimes = mapOf(
                        ComparisonName.PERSONAL_BEST to SplitTime(realTime = oldPb)
                    )
                )
            )
        )

        val updated = run.withCompletedAttempt(
            attemptId = 1,
            startedAt = null,
            endedAt = null,
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = TimeSpan.fromSeconds(400.0), // Worse than 300s
            splitTimes = listOf(TimeSpan.fromSeconds(400.0))
        )

        val pb = updated.segments[0].splitTimes[ComparisonName.PERSONAL_BEST]?.realTime
        assertEquals(300000L, pb?.totalMilliseconds)
    }

    @Test
    fun `withCompletedAttempt should update best segments even if run is reset`() {
        val run = Run(
            gameInfo = GameInfo("Game", "Category"),
            segments = listOf(
                Segment(
                    name = "S1",
                    bestSegmentTime = SplitTime(realTime = TimeSpan.fromSeconds(50.0))
                )
            )
        )

        val updated = run.withCompletedAttempt(
            attemptId = 1,
            startedAt = null,
            endedAt = null,
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = null, // Run reset
            splitTimes = listOf(TimeSpan.fromSeconds(40.0)) // 40s is better than 50s
        )

        val bestSeg = updated.segments[0].bestSegmentTime?.realTime
        assertEquals(40000L, bestSeg?.totalMilliseconds)
        assertEquals(1, updated.segments[0].segmentHistory.size)
        assertEquals(40000L, updated.segments[0].segmentHistory[0].time.realTime?.totalMilliseconds)
    }

    @Test
    fun `withCompletedAttempt should handle multiple segments correctly`() {
        val run = Run(
            gameInfo = GameInfo("Game", "Category"),
            segments = listOf(Segment("S1"), Segment("S2")),
            attemptCount = 0
        )

        // Us: S1=10s, S2=25s (total). Segments: S1=10s, S2=15s.
        val updated = run.withCompletedAttempt(
            attemptId = 1,
            startedAt = null,
            endedAt = null,
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = TimeSpan.fromSeconds(25.0),
            splitTimes = listOf(TimeSpan.fromSeconds(10.0), TimeSpan.fromSeconds(25.0))
        )

        assertEquals(10000L, updated.segments[0].segmentHistory[0].time.realTime?.totalMilliseconds)
        assertEquals(15000L, updated.segments[1].segmentHistory[0].time.realTime?.totalMilliseconds)
    }

    @Test
    fun `withCompletedAttempt should handle null finalTime as a reset`() {
        val run = Run(
            gameInfo = GameInfo("G", "C"),
            segments = listOf(Segment("S1")),
            attemptCount = 5
        )

        val updated = run.withCompletedAttempt(
            attemptId = 6,
            startedAt = null,
            endedAt = null,
            pauseTime = TimeSpan.ZERO,
            timingMethod = TimingMethod.REAL_TIME,
            finalTime = null, // Reset
            splitTimes = listOf(null) // No split recorded
        )

        assertEquals(6, updated.attemptCount)
        assertEquals(1, updated.attemptHistory.size)
        assertNull(updated.attemptHistory[0].realTime)
        assertEquals(0, updated.segments[0].segmentHistory.size)
    }
}
