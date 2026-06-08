package com.elg.speedruncompanion.ui.screen.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.speedruncompanion.R
import com.elg.speedruncompanion.domain.model.TimeSpan
import com.elg.speedruncompanion.domain.model.TimerColorMode
import com.elg.speedruncompanion.domain.model.TimerState
import com.elg.speedruncompanion.domain.service.TimerDisplayColorResolver
import com.elg.speedruncompanion.ui.theme.SpeedrunThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LayoutEditorViewModel = hiltViewModel()
) {
    val preferences by viewModel.layoutPreferences.collectAsState()
    val colors = SpeedrunThemeColors.colors
    var showColorModeMenu by remember { mutableStateOf(false) }

    val previewTime = TimeSpan.fromSeconds(330.5)
    val previewText = previewTime.formatted(preferences.timeFormat)

    val previewColorToken = TimerDisplayColorResolver.resolve(
        colorMode = preferences.colorMode,
        timerState = TimerState.Running(
            startTime = 0L,
            pauseAccumulator = 0L,
            currentSegmentIndex = 0,
            splitTimes = emptyList(),
            comparison = com.elg.speedruncompanion.domain.model.ComparisonName.PERSONAL_BEST
        ),
        delta = null
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layout_editor_title), color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.deepBackground)
            )
        },
        containerColor = colors.deepBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.layout_editor_preview_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.elevatedSurface, MaterialTheme.shapes.medium)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.Bold,
                    color = previewColorToken.toComposeColor(colors),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                stringResource(R.string.layout_editor_format_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.layout_editor_leading_zeros)) },
                supportingContent = { Text(stringResource(R.string.layout_editor_leading_zeros_desc)) },
                trailingContent = {
                    Switch(
                        checked = preferences.timeFormat.showLeadingZeros,
                        onCheckedChange = { viewModel.setShowLeadingZeros(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.layout_editor_show_fraction)) },
                supportingContent = { Text(stringResource(R.string.layout_editor_show_fraction_desc)) },
                trailingContent = {
                    Switch(
                        checked = preferences.timeFormat.showFraction,
                        onCheckedChange = { viewModel.setShowFraction(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            if (preferences.timeFormat.showFraction) {
                Text(
                    stringResource(R.string.layout_editor_decimal_places),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3).forEach { places ->
                        val selected = preferences.timeFormat.decimalPlaces == places
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDecimalPlaces(places) },
                            label = { Text(stringResource(R.string.layout_editor_decimal_places_value, places)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.layout_editor_color_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Box {
                val colorModeLabel = when (preferences.colorMode) {
                    TimerColorMode.DELTA -> stringResource(R.string.layout_editor_color_delta)
                    TimerColorMode.TIMER_STATE -> stringResource(R.string.layout_editor_color_state)
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.layout_editor_color_mode)) },
                    supportingContent = { Text(colorModeLabel) },
                    modifier = Modifier.clickable { showColorModeMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showColorModeMenu,
                    onDismissRequest = { showColorModeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(stringResource(R.string.layout_editor_color_delta))
                                Text(
                                    stringResource(R.string.layout_editor_color_delta_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        },
                        onClick = {
                            viewModel.setColorMode(TimerColorMode.DELTA)
                            showColorModeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(stringResource(R.string.layout_editor_color_state))
                                Text(
                                    stringResource(R.string.layout_editor_color_state_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        },
                        onClick = {
                            viewModel.setColorMode(TimerColorMode.TIMER_STATE)
                            showColorModeMenu = false
                        }
                    )
                }
            }

            if (preferences.colorMode == TimerColorMode.TIMER_STATE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.elevatedSurface, MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.layout_editor_state_colors_legend),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    StateColorLegend(
                        label = stringResource(R.string.layout_editor_state_idle),
                        token = com.elg.speedruncompanion.domain.model.TimerDisplayColorToken.STATE_IDLE
                    )
                    StateColorLegend(
                        label = stringResource(R.string.layout_editor_state_running),
                        token = com.elg.speedruncompanion.domain.model.TimerDisplayColorToken.STATE_RUNNING
                    )
                    StateColorLegend(
                        label = stringResource(R.string.layout_editor_state_paused),
                        token = com.elg.speedruncompanion.domain.model.TimerDisplayColorToken.STATE_PAUSED
                    )
                    StateColorLegend(
                        label = stringResource(R.string.layout_editor_state_finished),
                        token = com.elg.speedruncompanion.domain.model.TimerDisplayColorToken.STATE_FINISHED
                    )
                }
            }
        }
    }
}

@Composable
private fun StateColorLegend(
    label: String,
    token: com.elg.speedruncompanion.domain.model.TimerDisplayColorToken
) {
    val colors = SpeedrunThemeColors.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(token.toComposeColor(colors), MaterialTheme.shapes.small)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}
