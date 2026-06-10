package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerDisplayColorToken
import com.elg.swiftsplit.domain.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerDisplayColorResolverTest {

    @Test
    fun resolveDeltaColorWhenRunning() {
        val delta = Delta(TimeSpan(1000), Delta.Status.AHEAD_GAINING)
        val token = TimerDisplayColorResolver.resolve(
            colorMode = TimerColorMode.DELTA,
            timerState = TimerState.Running(0, 0, 0, emptyList(), ComparisonName.PERSONAL_BEST),
            delta = delta
        )
        assertEquals(TimerDisplayColorToken.AHEAD_GAINING, token)
    }

    @Test
    fun resolveStateColorWhenPaused() {
        val token = TimerDisplayColorResolver.resolve(
            colorMode = TimerColorMode.TIMER_STATE,
            timerState = TimerState.Paused(TimeSpan.ZERO, 0, emptyList(), ComparisonName.PERSONAL_BEST),
            delta = null
        )
        assertEquals(TimerDisplayColorToken.STATE_PAUSED, token)
    }

    @Test
    fun resolveDefaultWhenNoDelta() {
        val token = TimerDisplayColorResolver.resolve(
            colorMode = TimerColorMode.DELTA,
            timerState = TimerState.Idle,
            delta = null
        )
        assertEquals(TimerDisplayColorToken.DEFAULT, token)
    }

    @Test
    fun `resolve should return all state colors correctly`() {
        assertEquals(
            TimerDisplayColorToken.STATE_IDLE,
            TimerDisplayColorResolver.resolve(TimerColorMode.TIMER_STATE, TimerState.Idle, null)
        )
        assertEquals(
            TimerDisplayColorToken.STATE_RUNNING,
            TimerDisplayColorResolver.resolve(TimerColorMode.TIMER_STATE, TimerState.Running(0, 0, 0, emptyList(), ComparisonName.PERSONAL_BEST), null)
        )
        assertEquals(
            TimerDisplayColorToken.STATE_FINISHED,
            TimerDisplayColorResolver.resolve(TimerColorMode.TIMER_STATE, TimerState.Finished(TimeSpan.ZERO, emptyList(), ComparisonName.PERSONAL_BEST), null)
        )
    }

    @Test
    fun `resolve should return all delta status colors correctly`() {
        val statuses = mapOf(
            Delta.Status.AHEAD_GAINING to TimerDisplayColorToken.AHEAD_GAINING,
            Delta.Status.AHEAD_LOSING to TimerDisplayColorToken.AHEAD_LOSING,
            Delta.Status.BEHIND_GAINING to TimerDisplayColorToken.BEHIND_GAINING,
            Delta.Status.BEHIND_LOSING to TimerDisplayColorToken.BEHIND_LOSING,
            Delta.Status.BEST_SEGMENT to TimerDisplayColorToken.BEST_SEGMENT,
            Delta.Status.EXACT to TimerDisplayColorToken.DEFAULT
        )

        statuses.forEach { (status, expectedToken) ->
            val delta = Delta(TimeSpan.ZERO, status)
            val result = TimerDisplayColorResolver.resolve(TimerColorMode.DELTA, TimerState.Idle, delta)
            assertEquals("Failed for status $status", expectedToken, result)
        }
    }
}
