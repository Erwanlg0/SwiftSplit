package com.elg.speedruncompanion.ui.screen.timer.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elg.speedruncompanion.domain.model.TimerState
import com.elg.speedruncompanion.ui.theme.SpeedrunThemeColors

import androidx.compose.ui.res.stringResource
import com.elg.speedruncompanion.R

@Composable
fun TimerControls(
    timerState: TimerState,
    onStartSplit: () -> Unit,
    onPauseResume: () -> Unit,
    onUndo: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    isLastSplit: Boolean = false
) {
    val colors = SpeedrunThemeColors.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onReset,
            enabled = timerState !is TimerState.Idle
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.timer_control_reset),
                tint = if (timerState !is TimerState.Idle) colors.error else colors.textDisabled
            )
        }

        IconButton(
            onClick = onUndo,
            enabled = when (timerState) {
                is TimerState.Running -> timerState.currentSegmentIndex > 0
                is TimerState.Paused -> timerState.currentSegmentIndex > 0
                else -> false
            }
        ) {
            Icon(
                Icons.Default.Undo,
                contentDescription = stringResource(R.string.timer_control_undo),
                tint = when (timerState) {
                    is TimerState.Running -> if (timerState.currentSegmentIndex > 0) colors.warning else colors.textDisabled
                    is TimerState.Paused -> if (timerState.currentSegmentIndex > 0) colors.warning else colors.textDisabled
                    else -> colors.textDisabled
                }
            )
        }

        FilledIconButton(
            onClick = onStartSplit,
            modifier = Modifier.size(64.dp),
            enabled = timerState !is TimerState.Finished,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.success,
                disabledContainerColor = colors.textDisabled
            )
        ) {
            val icon = when {
                timerState is TimerState.Idle -> Icons.Default.PlayArrow
                isLastSplit -> Icons.Default.Check
                else -> Icons.Default.Flag
            }
            val contentDesc = when {
                timerState is TimerState.Idle -> stringResource(R.string.timer_control_start)
                isLastSplit -> stringResource(R.string.timer_control_finish)
                else -> stringResource(R.string.timer_control_split)
            }
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = onSkip,
            enabled = timerState is TimerState.Running
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.timer_control_skip),
                tint = if (timerState is TimerState.Running) colors.textPrimary else colors.textDisabled
            )
        }

        IconButton(
            onClick = onPauseResume,
            enabled = timerState is TimerState.Running || timerState is TimerState.Paused
        ) {
            Icon(
                imageVector = if (timerState is TimerState.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (timerState is TimerState.Paused) stringResource(R.string.timer_control_resume) else stringResource(R.string.timer_control_pause),
                tint = if (timerState is TimerState.Running || timerState is TimerState.Paused) colors.info else colors.textDisabled
            )
        }
    }
}
