package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerDisplayColorToken
import com.elg.swiftsplit.domain.model.TimerState

object TimerDisplayColorResolver {

    fun resolve(
        colorMode: TimerColorMode,
        timerState: TimerState,
        delta: Delta?
    ): TimerDisplayColorToken {
        return when (colorMode) {
            TimerColorMode.DELTA -> resolveDeltaColor(delta)
            TimerColorMode.TIMER_STATE -> resolveStateColor(timerState)
        }
    }

    private fun resolveDeltaColor(delta: Delta?): TimerDisplayColorToken {
        return when (delta?.status) {
            Delta.Status.AHEAD_GAINING -> TimerDisplayColorToken.AHEAD_GAINING
            Delta.Status.AHEAD_LOSING -> TimerDisplayColorToken.AHEAD_LOSING
            Delta.Status.BEHIND_LOSING -> TimerDisplayColorToken.BEHIND_LOSING
            Delta.Status.BEHIND_GAINING -> TimerDisplayColorToken.BEHIND_GAINING
            Delta.Status.BEST_SEGMENT -> TimerDisplayColorToken.BEST_SEGMENT
            else -> TimerDisplayColorToken.DEFAULT
        }
    }

    private fun resolveStateColor(timerState: TimerState): TimerDisplayColorToken {
        return when (timerState) {
            TimerState.Idle -> TimerDisplayColorToken.STATE_IDLE
            is TimerState.Running -> TimerDisplayColorToken.STATE_RUNNING
            is TimerState.Paused -> TimerDisplayColorToken.STATE_PAUSED
            is TimerState.Finished -> TimerDisplayColorToken.STATE_FINISHED
        }
    }
}
