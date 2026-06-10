package com.elg.swiftsplit.ui.screen.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.FullscreenOrientationPreset
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeFormatPattern
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerColorMode
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.service.TimerDisplayColorResolver
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack

private sealed class LayoutPreviewState {
    data object Idle : LayoutPreviewState()
    data object Running : LayoutPreviewState()
    data object Paused : LayoutPreviewState()
    data object Finished : LayoutPreviewState()
    data class DeltaPreview(val status: Delta.Status) : LayoutPreviewState()
}

private fun previewStatesFor(colorMode: TimerColorMode): List<LayoutPreviewState> = when (colorMode) {
    TimerColorMode.TIMER_STATE -> listOf(
        LayoutPreviewState.Idle,
        LayoutPreviewState.Running,
        LayoutPreviewState.Paused,
        LayoutPreviewState.Finished
    )
    TimerColorMode.DELTA -> listOf(
        LayoutPreviewState.DeltaPreview(Delta.Status.AHEAD_GAINING),
        LayoutPreviewState.DeltaPreview(Delta.Status.AHEAD_LOSING),
        LayoutPreviewState.DeltaPreview(Delta.Status.BEHIND_LOSING),
        LayoutPreviewState.DeltaPreview(Delta.Status.BEHIND_GAINING),
        LayoutPreviewState.DeltaPreview(Delta.Status.BEST_SEGMENT),
        LayoutPreviewState.DeltaPreview(Delta.Status.EXACT)
    )
}

private fun LayoutPreviewState.toTimerState(): TimerState = when (this) {
    LayoutPreviewState.Idle -> TimerState.Idle
    LayoutPreviewState.Running -> TimerState.Running(
        startTime = 0L,
        pauseAccumulator = 0L,
        currentSegmentIndex = 0,
        splitTimes = emptyList(),
        comparison = ComparisonName.PERSONAL_BEST
    )
    LayoutPreviewState.Paused -> TimerState.Paused(
        elapsedTime = TimeSpan.fromSeconds(330.5),
        currentSegmentIndex = 0,
        splitTimes = emptyList(),
        comparison = ComparisonName.PERSONAL_BEST
    )
    LayoutPreviewState.Finished -> TimerState.Finished(
        finalTime = TimeSpan.fromSeconds(330.5),
        splitTimes = emptyList(),
        comparison = ComparisonName.PERSONAL_BEST
    )
    is LayoutPreviewState.DeltaPreview -> TimerState.Running(
        startTime = 0L,
        pauseAccumulator = 0L,
        currentSegmentIndex = 0,
        splitTimes = emptyList(),
        comparison = ComparisonName.PERSONAL_BEST
    )
}

private fun LayoutPreviewState.toDelta(): Delta? = when (this) {
    is LayoutPreviewState.DeltaPreview -> Delta(TimeSpan.fromSeconds(1.5), status)
    else -> null
}

@Composable
private fun LayoutPreviewState.label(): String = when (this) {
    LayoutPreviewState.Idle -> stringResource(R.string.layout_editor_state_idle)
    LayoutPreviewState.Running -> stringResource(R.string.layout_editor_state_running)
    LayoutPreviewState.Paused -> stringResource(R.string.layout_editor_state_paused)
    LayoutPreviewState.Finished -> stringResource(R.string.layout_editor_state_finished)
    is LayoutPreviewState.DeltaPreview -> when (status) {
        Delta.Status.AHEAD_GAINING -> stringResource(R.string.layout_editor_delta_ahead_gaining)
        Delta.Status.AHEAD_LOSING -> stringResource(R.string.layout_editor_delta_ahead_losing)
        Delta.Status.BEHIND_LOSING -> stringResource(R.string.layout_editor_delta_behind_losing)
        Delta.Status.BEHIND_GAINING -> stringResource(R.string.layout_editor_delta_behind_gaining)
        Delta.Status.BEST_SEGMENT -> stringResource(R.string.layout_editor_delta_best_segment)
        Delta.Status.EXACT -> stringResource(R.string.layout_editor_delta_exact)
    }
}

private fun getPatternDisplayName(pat: TimeFormatPattern): String = when (pat) {
    TimeFormatPattern.HH_MM_SS_SS -> "HH:mm:ss.SS"
    TimeFormatPattern.HH_MM_SS_S -> "HH:mm:ss.S"
    TimeFormatPattern.HH_MM_SS -> "HH:mm:ss"
    TimeFormatPattern.OPT_HH_MM_SS_SS -> "[HH:]mm:ss.SS"
    TimeFormatPattern.OPT_HH_MM_SS_S -> "[HH:]mm:ss.S"
    TimeFormatPattern.OPT_HH_MM_SS -> "[HH:]mm:ss"
    TimeFormatPattern.OPT_HH_OPT_MM_SS_SS -> "[HH:][mm:]ss.SS"
    TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> "[HH:][mm:]ss.S"
    TimeFormatPattern.MM_SS_SS -> "mm:ss.SS"
    TimeFormatPattern.MM_SS_S -> "mm:ss.S"
    TimeFormatPattern.MM_SS -> "mm:ss"
    TimeFormatPattern.OPT_MM_SS_SS -> "[mm:]ss.SS"
    TimeFormatPattern.OPT_MM_SS_S -> "[mm:]ss.S"
    TimeFormatPattern.OPT_MM_SS -> "[mm:]ss"
    TimeFormatPattern.SS_SS -> "ss.SS"
    TimeFormatPattern.SS_S -> "ss.S"
    TimeFormatPattern.SS -> "ss"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LayoutEditorViewModel = hiltViewModel()
) {
    val preferences by viewModel.layoutPreferences.collectAsStateWithLifecycle()
    val colors = SwiftSplitThemeColors.colors
    var showColorModeMenu by remember { mutableStateOf(false) }
    var showOrientationMenu by remember { mutableStateOf(false) }
    var showSplitsDecimalsMenu by remember { mutableStateOf(false) }
    var showApproachThresholdMenu by remember { mutableStateOf(false) }
    var showCustomApproachThresholdDialog by remember { mutableStateOf(false) }
    var customApproachThresholdInput by remember { mutableStateOf("") }

    val previewStates = remember(preferences.colorMode) { previewStatesFor(preferences.colorMode) }
    var previewIndex by remember(preferences.colorMode) { mutableIntStateOf(0) }
    val previewState = previewStates[previewIndex % previewStates.size]

    val previewTime = TimeSpan.fromSeconds(330.5)
    val previewText = previewTime.formatted(preferences.timeFormat)
    val previewTimerState = previewState.toTimerState()
    val previewDelta = previewState.toDelta()

    val previewColorToken = TimerDisplayColorResolver.resolve(
        colorMode = preferences.colorMode,
        timerState = previewTimerState,
        delta = previewDelta
    )
    val previewComposeColor = previewColorToken.toComposeColorWithPrefs(colors, preferences)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layout_editor_title), color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
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
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.elevatedSurface)
                    .clickable {
                        previewIndex = (previewIndex + 1) % previewStates.size
                    }
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.Bold,
                    color = previewComposeColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = stringResource(R.string.layout_editor_preview_tap_hint, previewState.label()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                stringResource(R.string.layout_editor_format_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            var showPatternMenu by remember { mutableStateOf(false) }
            val currentPattern = preferences.timeFormat.pattern
            val sampleShort = TimeSpan.fromSeconds(1.23)
            val sampleLong = TimeSpan.fromHours(1.0) + TimeSpan.fromMinutes(5.0) + TimeSpan.fromSeconds(30.45)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.layout_editor_timer_format),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textPrimary
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = getPatternDisplayName(currentPattern),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = stringResource(
                                        R.string.layout_editor_format_examples,
                                        sampleShort.formatted(preferences.timeFormat),
                                        sampleLong.formatted(preferences.timeFormat)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        modifier = Modifier.clickable { showPatternMenu = true },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                    DropdownMenu(
                        expanded = showPatternMenu,
                        onDismissRequest = { showPatternMenu = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                    TimeFormatPattern.entries.forEach { pat ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        getPatternDisplayName(pat),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "${sampleShort.formatted(TimeFormatOptions(pattern = pat))} · ${sampleLong.formatted(TimeFormatOptions(pattern = pat))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setFormatPattern(pat)
                                showPatternMenu = false
                            }
                        )
                    }
                }
            }
        }

            Text(
                stringResource(R.string.layout_editor_splits_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_splits), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.layout_editor_show_splits_desc), color = colors.textSecondary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showSplits,
                                onCheckedChange = { viewModel.setShowSplits(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.layout_editor_orientation), color = colors.textPrimary) },
                            supportingContent = {
                                Text(
                                    text = when (preferences.fullscreenOrientation) {
                                        FullscreenOrientationPreset.PORTRAIT -> stringResource(R.string.layout_editor_orientation_portrait)
                                        FullscreenOrientationPreset.LANDSCAPE -> stringResource(R.string.layout_editor_orientation_landscape)
                                        FullscreenOrientationPreset.AUTO -> stringResource(R.string.layout_editor_orientation_auto)
                                    },
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.clickable { showOrientationMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showOrientationMenu,
                            onDismissRequest = { showOrientationMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            FullscreenOrientationPreset.entries.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (preset) {
                                                FullscreenOrientationPreset.PORTRAIT -> stringResource(R.string.layout_editor_orientation_portrait)
                                                FullscreenOrientationPreset.LANDSCAPE -> stringResource(R.string.layout_editor_orientation_landscape)
                                                FullscreenOrientationPreset.AUTO -> stringResource(R.string.layout_editor_orientation_auto)
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setFullscreenOrientation(preset)
                                        showOrientationMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_splits_fraction), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.layout_editor_splits_fraction_desc), color = colors.textSecondary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showSplitsFraction,
                                onCheckedChange = { viewModel.setShowSplitsFraction(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.layout_editor_splits_decimals), color = colors.textPrimary) },
                            supportingContent = {
                                Text(
                                    text = stringResource(
                                        R.string.layout_editor_decimal_places_value,
                                        preferences.splitsDecimalPlaces
                                    ),
                                    color = colors.textSecondary
                                )
                            },
                            modifier = Modifier.clickable { showSplitsDecimalsMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showSplitsDecimalsMenu,
                            onDismissRequest = { showSplitsDecimalsMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            (0..3).forEach { places ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_editor_decimal_places_value, places)) },
                                    onClick = {
                                        viewModel.setSplitsDecimalPlaces(places)
                                        showSplitsDecimalsMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.layout_editor_split_approach_threshold), color = colors.textPrimary)
                            },
                            supportingContent = {
                                Text(
                                    text = if (preferences.splitApproachThresholdSeconds == 0) {
                                        stringResource(R.string.layout_editor_split_approach_threshold_disabled)
                                    } else {
                                        stringResource(
                                            R.string.layout_editor_split_approach_threshold_value,
                                            preferences.splitApproachThresholdSeconds
                                        )
                                    },
                                    color = colors.textSecondary
                                )
                            },
                            modifier = Modifier.clickable { showApproachThresholdMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showApproachThresholdMenu,
                            onDismissRequest = { showApproachThresholdMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            listOf(0, 10, 15, 20, 30, 45, 60).forEach { seconds ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (seconds == 0) {
                                                stringResource(R.string.layout_editor_split_approach_threshold_disabled)
                                            } else {
                                                stringResource(
                                                    R.string.layout_editor_split_approach_threshold_value,
                                                    seconds
                                                )
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSplitApproachThresholdSeconds(seconds)
                                        showApproachThresholdMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.layout_editor_split_approach_threshold_custom)) },
                                onClick = {
                                    customApproachThresholdInput = if (preferences.splitApproachThresholdSeconds > 0) {
                                        preferences.splitApproachThresholdSeconds.toString()
                                    } else {
                                        ""
                                    }
                                    showApproachThresholdMenu = false
                                    showCustomApproachThresholdDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (showCustomApproachThresholdDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomApproachThresholdDialog = false },
                    title = { Text(stringResource(R.string.layout_editor_split_approach_threshold_custom_title)) },
                    text = {
                        OutlinedTextField(
                            value = customApproachThresholdInput,
                            onValueChange = { customApproachThresholdInput = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text(stringResource(R.string.layout_editor_split_approach_threshold_custom_hint)) },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val seconds = customApproachThresholdInput.toIntOrNull()?.coerceIn(0, 120) ?: return@TextButton
                                viewModel.setSplitApproachThresholdSeconds(seconds)
                                showCustomApproachThresholdDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomApproachThresholdDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            Text(
                stringResource(R.string.layout_editor_controls_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_undo), color = colors.textPrimary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showUndoButton,
                                onCheckedChange = { viewModel.setShowUndoButton(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_skip), color = colors.textPrimary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showSkipButton,
                                onCheckedChange = { viewModel.setShowSkipButton(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_pause), color = colors.textPrimary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showPauseButton,
                                onCheckedChange = { viewModel.setShowPauseButton(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_enable_vibration), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.layout_editor_enable_vibration_desc), color = colors.textSecondary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.enableVibration,
                                onCheckedChange = { viewModel.setEnableVibration(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_sob), color = colors.textPrimary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showSumOfBest,
                                onCheckedChange = { viewModel.setShowSumOfBest(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_show_durations), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.layout_editor_show_durations_desc), color = colors.textSecondary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.showSegmentDurations,
                                onCheckedChange = { viewModel.setShowSegmentDurations(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_minimalist), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.layout_editor_minimalist_desc), color = colors.textSecondary) },
                        trailingContent = {
                            Switch(
                                checked = preferences.isMinimalistMode,
                                onCheckedChange = { viewModel.setIsMinimalistMode(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            Text(
                stringResource(R.string.layout_editor_color_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val colorModeLabel = when (preferences.colorMode) {
                        TimerColorMode.DELTA -> stringResource(R.string.layout_editor_color_delta)
                        TimerColorMode.TIMER_STATE -> stringResource(R.string.layout_editor_color_state)
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_editor_color_mode), color = colors.textPrimary) },
                        supportingContent = { Text(colorModeLabel, color = colors.textSecondary) },
                        modifier = Modifier.clickable { showColorModeMenu = true },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                    DropdownMenu(
                        expanded = showColorModeMenu,
                        onDismissRequest = { showColorModeMenu = false },
                        shape = RoundedCornerShape(12.dp)
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
            }

            if (preferences.colorMode == TimerColorMode.TIMER_STATE) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            color = colors.timerTextDim,
                            preset = null,
                            onPresetSelected = {}
                        )
                        StateColorLegend(
                            label = stringResource(R.string.layout_editor_state_running),
                            color = preferences.stateColorRunning.toComposeColor(colors),
                            preset = preferences.stateColorRunning,
                            onPresetSelected = { viewModel.setRunningStateColor(it) }
                        )
                        StateColorLegend(
                            label = stringResource(R.string.layout_editor_state_paused),
                            color = preferences.stateColorPaused.toComposeColor(colors),
                            preset = preferences.stateColorPaused,
                            onPresetSelected = { viewModel.setPausedStateColor(it) }
                        )
                        StateColorLegend(
                            label = stringResource(R.string.layout_editor_state_finished),
                            color = preferences.stateColorFinished.toComposeColor(colors),
                            preset = preferences.stateColorFinished,
                            onPresetSelected = { viewModel.setFinishedStateColor(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateColorLegend(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    preset: com.elg.swiftsplit.domain.model.StateColorPreset?,
    onPresetSelected: (com.elg.swiftsplit.domain.model.StateColorPreset) -> Unit
) {
    val colors = SwiftSplitThemeColors.colors
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = preset != null) { expanded = true }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(color, MaterialTheme.shapes.small)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                if (preset != null) {
                    Text(
                        text = stringResource(R.string.layout_editor_preset_format, preset.localName()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            if (preset != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Modifier",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (preset != null) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(12.dp)
            ) {
                com.elg.swiftsplit.domain.model.StateColorPreset.entries.forEach { pr ->
                    DropdownMenuItem(
                        text = { Text(pr.localName()) },
                        onClick = {
                            onPresetSelected(pr)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun com.elg.swiftsplit.domain.model.StateColorPreset.localName(): String = when (this) {
    com.elg.swiftsplit.domain.model.StateColorPreset.GREEN -> stringResource(R.string.color_preset_green)
    com.elg.swiftsplit.domain.model.StateColorPreset.BLUE -> stringResource(R.string.color_preset_blue)
    com.elg.swiftsplit.domain.model.StateColorPreset.GRAY -> stringResource(R.string.color_preset_gray)
    com.elg.swiftsplit.domain.model.StateColorPreset.RED -> stringResource(R.string.color_preset_red)
    com.elg.swiftsplit.domain.model.StateColorPreset.ORANGE -> stringResource(R.string.color_preset_orange)
    com.elg.swiftsplit.domain.model.StateColorPreset.GOLD -> stringResource(R.string.color_preset_gold)
    com.elg.swiftsplit.domain.model.StateColorPreset.WHITE -> stringResource(R.string.color_preset_white)
}

