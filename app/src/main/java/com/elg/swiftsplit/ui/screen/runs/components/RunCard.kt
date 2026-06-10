package com.elg.swiftsplit.ui.screen.runs.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R

import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RunCard(
    run: Run,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swiftSplitColors = SwiftSplitThemeColors.colors
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEditClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = swiftSplitColors.cardBackground
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(swiftSplitColors.cardBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = run.gameInfo.gameName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = swiftSplitColors.textPrimary
                    )
                    Text(
                        text = run.gameInfo.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = swiftSplitColors.textSecondary
                    )
                }
 
                FilledIconButton(
                    onClick = onClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.timer_control_start))
                }
            }
 
            Spacer(modifier = Modifier.height(12.dp))
 
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (run.gameInfo.platform.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(run.gameInfo.platform) },
                            enabled = false,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = stringResource(com.elg.swiftsplit.R.string.timer_attempts, run.attemptCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = swiftSplitColors.textTertiary
                    )
                }
 
                val pbTime = run.personalBest?.getTime(TimingMethod.REAL_TIME)
                Text(
                    text = pbTime?.formatted() ?: stringResource(com.elg.swiftsplit.R.string.runs_list_no_pb),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = pbTime?.let { swiftSplitColors.aheadGaining } ?: swiftSplitColors.textTertiary
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = swiftSplitColors.textSecondary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = swiftSplitColors.error
                    )
                }
            }
        }
    }
}
