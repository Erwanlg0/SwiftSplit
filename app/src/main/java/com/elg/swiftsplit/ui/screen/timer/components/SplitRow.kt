package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.Segment
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.domain.service.SplitTimeCalculator
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors

@Composable
fun SplitRow(
    segment: Segment,
    isActive: Boolean,
    isCompleted: Boolean,
    elapsedSplit: TimeSpan?,
    comparisonName: String,
    timingMethod: TimingMethod,
    previousCurrentSplit: TimeSpan?,
    previousComparisonSplit: TimeSpan?,
    timeFormat: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    layoutPreferences: TimerLayoutPreferences = TimerLayoutPreferences.DEFAULT,
    liveDelta: Delta? = null,
    liveElapsed: TimeSpan? = null,
    modifier: Modifier = Modifier
) {
    val colors = SwiftSplitThemeColors.colors
    val compSplit = segment.splitTimes[com.elg.swiftsplit.domain.model.ComparisonName(comparisonName)]?.getTime(timingMethod)

            val delta = if (isCompleted && elapsedSplit != null) {
        val segmentVal = previousCurrentSplit?.let { elapsedSplit - it } ?: elapsedSplit
        val isGold = segment.bestSegmentTime?.getTime(timingMethod)?.let { segmentVal <= it } ?: false

        SplitTimeCalculator.getDelta(
            currentSplit = elapsedSplit,
            comparisonSplit = compSplit,
            previousCurrentSplit = previousCurrentSplit,
            previousComparisonSplit = previousComparisonSplit,
            isBestSegment = isGold
        )
    } else if (isActive && liveDelta != null) {
        liveDelta
    } else {
        null
    }

    val pts = if (isActive && !isCompleted) {
        val pbSplit = compSplit
        val prevPbSplit = previousComparisonSplit ?: TimeSpan.ZERO
        val pbSegment = pbSplit?.let { it - prevPbSplit }
        val bestSegment = segment.bestSegmentTime?.getTime(timingMethod)
        
        if (pbSegment != null && bestSegment != null) {
            pbSegment - bestSegment
        } else {
            null
        }
    } else {
        null
    }


    val deltaColor = when (delta?.status) {
        Delta.Status.AHEAD_GAINING -> colors.aheadGaining
        Delta.Status.AHEAD_LOSING -> colors.aheadLosing
        Delta.Status.BEHIND_LOSING -> colors.behindLosing
        Delta.Status.BEHIND_GAINING -> colors.behindGaining
        Delta.Status.BEST_SEGMENT -> colors.bestSegment
        else -> colors.textSecondary
    }

    val activeBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1D51A7),
            Color(0xFF1D51A7).copy(alpha = 0.2f)
        )
    )

    Column(modifier = modifier.fillMaxWidth().graphicsLayer(alpha = layoutPreferences.segmentOpacity)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) activeBrush else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val iconData = segment.iconData
            if (iconData != null && iconData.isNotBlank()) {
                val bitmap = remember(iconData) {
                    com.elg.swiftsplit.infrastructure.parser.LssIconDecoder.decode(iconData)?.asImageBitmap()
                }
                bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                }
            }

            Text(
                text = if (layoutPreferences.useSubsplits && segment.isSubsplit) "  ${segment.cleanedName}" else segment.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color.White else colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = delta?.time?.formattedWithSign(
                    showMilliseconds = layoutPreferences.showSplitsFraction,
                    decimalPlaces = layoutPreferences.splitsDecimalPlaces
                ) ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                fontWeight = FontWeight.Bold,
                color = deltaColor,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            val timeToShow = when {
                liveElapsed != null -> liveElapsed.formatted(timeFormat)
                isCompleted && elapsedSplit != null -> {
                    if (layoutPreferences.showSegmentDurations) {
                        (previousCurrentSplit?.let { elapsedSplit - it } ?: elapsedSplit).formatted(timeFormat)
                    } else {
                        elapsedSplit.formatted(timeFormat)
                    }
                }
                pts != null && layoutPreferences.showPossibleTimeSave -> "-${pts.formatted(false)}"
                else -> compSplit?.formatted(timeFormat) ?: "-"
            }

            val timeColor = when {
                liveElapsed != null && liveDelta != null -> deltaColor
                isActive && pts != null && layoutPreferences.showPossibleTimeSave -> colors.textTertiary
                isActive -> Color.White
                else -> colors.textSecondary
            }

            Text(
                text = timeToShow,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                fontWeight = if (isActive || liveElapsed != null) FontWeight.Bold else FontWeight.Normal,
                color = timeColor,
                modifier = Modifier.width(90.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        androidx.compose.material3.HorizontalDivider(
            color = Color(0x1AFFFFFF),
            thickness = 0.5.dp
        )
    }
}
