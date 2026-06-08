package com.elg.speedruncompanion.ui.screen.timer.components

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
import com.elg.speedruncompanion.domain.model.Delta
import com.elg.speedruncompanion.domain.model.TimeFormatOptions
import com.elg.speedruncompanion.domain.model.TimeSpan
import com.elg.speedruncompanion.domain.model.TimerColorMode
import com.elg.speedruncompanion.domain.model.TimerState
import com.elg.speedruncompanion.domain.service.TimerDisplayColorResolver
import com.elg.speedruncompanion.ui.screen.layout.toComposeColor
import com.elg.speedruncompanion.ui.theme.SpeedrunThemeColors

@Composable
fun TimerDisplay(
    elapsedTime: TimeSpan,
    delta: Delta?,
    timerState: TimerState,
    timeFormat: TimeFormatOptions,
    colorMode: TimerColorMode,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val colors = SpeedrunThemeColors.colors
    val colorToken = TimerDisplayColorResolver.resolve(colorMode, timerState, delta)
    val timerColor = colorToken.toComposeColor(colors)

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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    )
}
