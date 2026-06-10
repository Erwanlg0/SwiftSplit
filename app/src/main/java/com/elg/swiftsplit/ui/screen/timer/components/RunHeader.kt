package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R
import androidx.compose.ui.res.stringResource

@Composable
fun RunHeader(
    run: Run,
    modifier: Modifier = Modifier,
    activeComparison: String? = null,
    onComparisonClick: (() -> Unit)? = null,
    backgroundColor: Color = SwiftSplitThemeColors.colors.elevatedSurface
) {
    val swiftSplitColors = SwiftSplitThemeColors.colors

    val iconData = run.gameInfo.iconData
    val gameIcon = remember(iconData) {
        com.elg.swiftsplit.infrastructure.parser.LssIconDecoder.decode(iconData)?.asImageBitmap()
    }
 
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (gameIcon != null) {
                    Image(
                        bitmap = gameIcon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).padding(end = 12.dp)
                    )
                }

                Column {
                    Text(
                        text = run.gameInfo.gameName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = swiftSplitColors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.then(
                            if (onComparisonClick != null) Modifier.clickable { onComparisonClick() }
                            else Modifier
                        )
                    ) {
                        Text(
                            text = run.gameInfo.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = swiftSplitColors.textSecondary
                        )
                        if (activeComparison != null) {
                            Text(
                                text = " • $activeComparison",
                                style = MaterialTheme.typography.titleSmall,
                                color = swiftSplitColors.textTertiary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
             
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.timer_attempts_label),
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
