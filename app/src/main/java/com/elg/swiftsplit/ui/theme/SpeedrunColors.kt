package com.elg.swiftsplit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf


@Immutable
data class SpeedrunColorScheme(
    val aheadGaining: Color = AheadGaining,
    val aheadLosing: Color = AheadLosing,
    val behindLosing: Color = BehindLosing,
    val behindGaining: Color = BehindGaining,
    val bestSegment: Color = GoldSplit,
    val bestSegmentBright: Color = GoldSplitBright,
    val timerText: Color = TimerWhite,
    val timerTextDim: Color = TimerGray,
    val cardBackground: Color = CardDark,
    val cardBorder: Color = BorderDark,
    val elevatedSurface: Color = ElevatedDark,
    val deepBackground: Color = DeepBlack,
    val success: Color = SuccessGreen,
    val error: Color = ErrorRed,
    val warning: Color = WarningOrange,
    val info: Color = InfoBlue,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val textDisabled: Color = TextDisabled,
)

val LocalSpeedrunColors = staticCompositionLocalOf { SpeedrunColorScheme() }
