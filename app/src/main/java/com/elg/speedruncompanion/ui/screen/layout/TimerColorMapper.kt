package com.elg.speedruncompanion.ui.screen.layout

import com.elg.speedruncompanion.domain.model.TimerDisplayColorToken
import com.elg.speedruncompanion.ui.theme.SpeedrunColorScheme

fun TimerDisplayColorToken.toComposeColor(colors: SpeedrunColorScheme) = when (this) {
    TimerDisplayColorToken.DEFAULT -> colors.timerText
    TimerDisplayColorToken.AHEAD_GAINING -> colors.aheadGaining
    TimerDisplayColorToken.AHEAD_LOSING -> colors.aheadLosing
    TimerDisplayColorToken.BEHIND_LOSING -> colors.behindLosing
    TimerDisplayColorToken.BEHIND_GAINING -> colors.behindGaining
    TimerDisplayColorToken.BEST_SEGMENT -> colors.bestSegment
    TimerDisplayColorToken.STATE_IDLE -> colors.timerTextDim
    TimerDisplayColorToken.STATE_RUNNING -> colors.success
    TimerDisplayColorToken.STATE_PAUSED -> colors.timerTextDim
    TimerDisplayColorToken.STATE_FINISHED -> colors.info
}
