package com.elg.speedruncompanion.domain.service

import com.elg.speedruncompanion.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TimerServiceTest {
    private val timerService = TimerService()

    @Test
    fun testTimerStart() {
        val run = Run(
            gameInfo = GameInfo("Super Mario 64", "16 Star"),
            segments = listOf(Segment("Bob-omb Battlefield"), Segment("Whomps Fortress"))
        )
        val active = timerService.start(run, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        
        assertNotNull(active.startTime)
        assertEquals(0, active.currentSegmentIndex)
        assertEquals(2, active.splitTimes.size)
    }
}
