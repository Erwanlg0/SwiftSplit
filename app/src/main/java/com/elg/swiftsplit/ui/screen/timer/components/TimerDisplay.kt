package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.service.TimerDisplayColorResolver
import com.elg.swiftsplit.ui.screen.layout.toComposeColorWithPrefs
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors

@Composable
fun TimerDisplay(
    elapsedTime: TimeSpan,
    delta: Delta?,
    timerState: TimerState,
    timeFormat: TimeFormatOptions,
    colorMode: TimerColorMode,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    layoutPreferences: TimerLayoutPreferences = TimerLayoutPreferences.DEFAULT
) {
    val colors = SwiftSplitThemeColors.colors
    val colorToken = TimerDisplayColorResolver.resolve(colorMode, timerState, delta)
    val timerColor = colorToken.toComposeColorWithPrefs(colors, layoutPreferences)

    val style = if (fontSize != TextUnit.Unspecified) {
        MaterialTheme.typography.displayLarge.copy(fontSize = fontSize)
    } else {
        MaterialTheme.typography.displayLarge
    }

    Text(
        text = elapsedTime.formatted(timeFormat),
        style = style,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = timerColor,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    )
}

