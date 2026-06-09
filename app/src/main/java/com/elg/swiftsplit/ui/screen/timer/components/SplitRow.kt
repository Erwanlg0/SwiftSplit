package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val rowBackground = if (isActive) colors.elevatedSurface else Color.Transparent
    val rowBorder = if (isActive) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(rowBorder)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = segment.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = delta?.time?.formattedWithSign(
                showMilliseconds = layoutPreferences.showSplitsFraction,
                decimalPlaces = layoutPreferences.splitsDecimalPlaces
            ) ?: "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = deltaColor,
            modifier = Modifier.width(80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        val timeToShow = if (isCompleted && elapsedSplit != null) {
            elapsedSplit.formatted(timeFormat)
        } else {
            compSplit?.formatted(timeFormat) ?: "-"
        }
        
        Text(
            text = timeToShow,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.width(90.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
