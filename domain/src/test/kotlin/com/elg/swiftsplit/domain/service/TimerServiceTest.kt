package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerServiceTest {
    private val clock: Clock = mockk()
    private val timerService = TimerService(clock)

    @Test
    fun testTimerStart() {
        every { clock.currentTimeMillis() } returns 1000L
        val run = Run(
            gameInfo = GameInfo("Super Mario 64", "16 Star"),
            segments = listOf(Segment("Bob-omb Battlefield"), Segment("Whomps Fortress"))
        )
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        assertEquals(1000L, active.startTime)
        assertEquals(0, active.currentSegmentIndex)
        assertEquals(2, active.splitTimes.size)
    }

    @Test
    fun testSplit() {
        every { clock.currentTimeMillis() } returns 1000L
        val run = Run(
            gameInfo = GameInfo("Test", "Test"),
            segments = listOf(Segment("S1"), Segment("S2"))
        )
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        every { clock.currentTimeMillis() } returns 2500L
        val (updated, event) = timerService.split(active)
        
        assertEquals(1, updated.currentSegmentIndex)
        assertEquals(1500L, updated.splitTimes[0]?.totalMilliseconds)
        assertTrue(event is TimerEvent.Split)
        assertEquals(1500L, (event as TimerEvent.Split).splitTime.totalMilliseconds)
    }

    @Test
    fun testSkip() {
        every { clock.currentTimeMillis() } returns 1000L
        val run = Run(
            gameInfo = GameInfo("Test", "Test"),
            segments = listOf(Segment("S1"), Segment("S2"))
        )
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        val (updated, event) = timerService.skip(active)
        
        assertEquals(1, updated.currentSegmentIndex)
        assertEquals(null, updated.splitTimes[0])
        assertTrue(event is TimerEvent.Skipped)
        assertEquals(0, (event as TimerEvent.Skipped).segmentIndex)
    }

    @Test
    fun testUndoSplit() {
        every { clock.currentTimeMillis() } returns 1000L
        val run = Run(gameInfo = GameInfo("T", "C"), segments = listOf(Segment("S1"), Segment("S2")))
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        every { clock.currentTimeMillis() } returns 2000L
        val (splitted, _) = timerService.split(active)
        assertEquals(1, splitted.currentSegmentIndex)
        
        val (undone, event) = timerService.undoSplit(splitted)
        assertEquals(0, undone.currentSegmentIndex)
        assertEquals(null, undone.splitTimes[0])
        assertTrue(event is TimerEvent.Undone)
    }

    @Test
    fun testPauseAndResume() {
        every { clock.currentTimeMillis() } returns 1000L
        val run = Run(gameInfo = GameInfo("T", "C"), segments = listOf(Segment("S1")))
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        // Pause at 2000ms (1s elapsed)
        every { clock.currentTimeMillis() } returns 2000L
        val (paused, _) = timerService.pause(active)
        assertEquals(2000L, paused.pauseStart)
        
        // Resume at 3000ms (1s paused)
        every { clock.currentTimeMillis() } returns 3000L
        val (resumed, _) = timerService.resume(paused)
        assertEquals(null, resumed.pauseStart)
        assertEquals(1000L, resumed.pauseAccumulator)
        
        // Check elapsed at 5000ms
        // Total time 4s - 1s pause = 3s
        assertEquals(3000L, timerService.getElapsedTime(resumed, 5000L).totalMilliseconds)
    }
}


