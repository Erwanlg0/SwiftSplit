package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors

import androidx.compose.ui.res.stringResource

@Composable
fun RunHeader(
    run: Run,
    modifier: Modifier = Modifier
) {
    val swiftSplitColors = SwiftSplitThemeColors.colors
 
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(swiftSplitColors.elevatedSurface)
            .padding(16.dp)
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
             
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(com.elg.swiftsplit.R.string.timer_attempts_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = swiftSplitColors.textTertiary
                )
                Text(
                    text = run.attemptCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = swiftSplitColors.textPrimary
                )
            }
        }
    }
}
