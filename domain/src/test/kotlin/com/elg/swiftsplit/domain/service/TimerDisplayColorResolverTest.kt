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
}
