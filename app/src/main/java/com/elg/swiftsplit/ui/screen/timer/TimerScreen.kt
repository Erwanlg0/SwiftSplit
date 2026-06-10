package com.elg.swiftsplit.ui.screen.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.service.SplitTimeCalculator
import com.elg.swiftsplit.domain.model.FullscreenOrientationPreset
import com.elg.swiftsplit.ui.screen.layout.toComposeColorWithPrefs
import com.elg.swiftsplit.domain.service.TimerDisplayColorResolver
import com.elg.swiftsplit.ui.screen.timer.components.RunHeader
import com.elg.swiftsplit.ui.screen.timer.components.SplitList
import com.elg.swiftsplit.ui.screen.timer.components.TimerControls
import com.elg.swiftsplit.ui.screen.timer.components.TimerDisplay
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun computeCurrentDelta(
    timerState: TimerState,
    currentRun: Run,
    currentIndex: Int,
    currentElapsed: TimeSpan,
    activeComp: String
): Delta? {
    if (timerState !is TimerState.Running || currentIndex >= currentRun.segments.size) return null

    val activeCompName = ComparisonName(activeComp)
    val currentSegment = currentRun.segments[currentIndex]
    val compSplit = currentSegment.splitTimes[activeCompName]?.getTime(TimingMethod.REAL_TIME)
    val splitTimes = timerState.splitTimes

    val previousCurrentSplit = if (currentIndex > 0) splitTimes[currentIndex - 1] else null
    val previousComparisonSplit = if (currentIndex > 0) {
        currentRun.segments[currentIndex - 1].splitTimes[activeCompName]?.getTime(TimingMethod.REAL_TIME)
    } else {
        TimeSpan.ZERO
    }

    return SplitTimeCalculator.getDelta(
        currentSplit = currentElapsed,
        comparisonSplit = compSplit,
        previousCurrentSplit = previousCurrentSplit,
        previousComparisonSplit = previousComparisonSplit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    runId: String,
    onNavigateBack: () -> Unit,
    onEditSplits: (String) -> Unit,
    onEditLayout: () -> Unit,
    onNavigateToStats: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val run by viewModel.run.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val currentElapsed by viewModel.currentElapsed.collectAsStateWithLifecycle()
    val layoutPreferences by viewModel.layoutPreferences.collectAsStateWithLifecycle()

    val colors = SwiftSplitThemeColors.colors
    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showSplitsInFullscreen by rememberSaveable(layoutPreferences.showSplits) {
        mutableStateOf(layoutPreferences.showSplits)
    }

    val backgroundModifier = if (layoutPreferences.backgroundGradientEnabled) {
        Modifier.background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color(layoutPreferences.backgroundGradientStart.toColorInt()),
                    androidx.compose.ui.graphics.Color(layoutPreferences.backgroundGradientEnd.toColorInt())
                )
            )
        )
    } else {
        Modifier.background(colors.deepBackground)
    }

    
    DisposableEffect(timerState) {
        val activity = context as? Activity
        if (timerState is TimerState.Running) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val requestedOrientation = when (layoutPreferences.fullscreenOrientation) {
        FullscreenOrientationPreset.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        FullscreenOrientationPreset.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        FullscreenOrientationPreset.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    
    DisposableEffect(isFullscreen, layoutPreferences.fullscreenOrientation) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = requestedOrientation
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (isFullscreen && run != null) {
        val currentRun = run!!
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isPortrait = when (layoutPreferences.fullscreenOrientation) {
            FullscreenOrientationPreset.PORTRAIT -> true
            FullscreenOrientationPreset.LANDSCAPE -> false
            FullscreenOrientationPreset.AUTO ->
                configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .pointerInput(timerState, layoutPreferences.isMinimalistMode) {
                    detectTapGestures(
                        onTap = {
                            when (timerState) {
                                is TimerState.Idle -> viewModel.startTimer()
                                is TimerState.Running -> {
                                    if (layoutPreferences.isMinimalistMode) {
                                        viewModel.split()
                                    } else {
                                        viewModel.pauseResume()
                                    }
                                }
                                is TimerState.Paused -> viewModel.pauseResume()
                                else -> {}
                            }
                        },
                        onLongPress = {
                            if (layoutPreferences.isMinimalistMode && timerState is TimerState.Running) {
                                viewModel.pauseResume()
                            }
                        }
                    )
                }
        ) {
            val currentIndex = when (val state = timerState) {
                is TimerState.Running -> state.currentSegmentIndex
                is TimerState.Paused -> state.currentSegmentIndex
                is TimerState.Finished -> currentRun.segments.size
                else -> 0
            }
            val activeComp = when (val state = timerState) {
                is TimerState.Running -> state.comparison.name
                is TimerState.Paused -> state.comparison.name
                is TimerState.Finished -> state.comparison.name
                else -> "Personal Best"
            }
            val splitTimes = when (val state = timerState) {
                is TimerState.Running -> state.splitTimes
                is TimerState.Paused -> state.splitTimes
                is TimerState.Finished -> state.splitTimes
                else -> List(currentRun.segments.size) { null }
            }

            val currentDelta = computeCurrentDelta(
                timerState = timerState,
                currentRun = currentRun,
                currentIndex = currentIndex,
                currentElapsed = currentElapsed,
                activeComp = activeComp
            )

            val timerColor = TimerDisplayColorResolver.resolve(
                colorMode = layoutPreferences.colorMode,
                timerState = timerState,
                delta = currentDelta
            ).toComposeColorWithPrefs(colors, layoutPreferences)

            if (isPortrait) {
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (layoutPreferences.isMinimalistMode) 16.dp else 64.dp,
                            bottom = 24.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    
                    if (!layoutPreferences.isMinimalistMode) {
                        Text(
                            text = "${currentRun.gameInfo.gameName} — ${currentRun.gameInfo.categoryName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    if (showSplitsInFullscreen) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SplitList(
                            run = currentRun,
                            currentSegmentIndex = currentIndex,
                            splitTimes = splitTimes,
                            comparisonName = activeComp,
                            timingMethod = TimingMethod.REAL_TIME,
                            timeFormat = layoutPreferences.timeFormat,
                            layoutPreferences = layoutPreferences,
                            modifier = Modifier.weight(1f),
                            completedSplitsVisible = 2,
                            currentElapsed = currentElapsed,
                            activeSegmentDelta = currentDelta,
                            isTimerRunning = timerState is TimerState.Running
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentElapsed.formatted(layoutPreferences.timeFormat),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (showSplitsInFullscreen) 80.sp else 120.sp,
                            lineHeight = if (showSplitsInFullscreen) 88.sp else 132.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = timerColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        softWrap = true
                    )

                    if (!layoutPreferences.isMinimalistMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val stateText = when (timerState) {
                            is TimerState.Running -> stringResource(R.string.phase_running)
                            is TimerState.Paused -> stringResource(R.string.phase_paused)
                            is TimerState.Finished -> stringResource(R.string.phase_ended)
                            else -> stringResource(R.string.phase_not_running)
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (timerState) {
                                is TimerState.Running -> colors.success.copy(alpha = 0.2f)
                                is TimerState.Paused -> colors.warning.copy(alpha = 0.2f)
                                is TimerState.Finished -> colors.info.copy(alpha = 0.2f)
                                else -> colors.textDisabled.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = stateText.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (timerState) {
                                    is TimerState.Running -> colors.success
                                    is TimerState.Paused -> colors.warning
                                    is TimerState.Finished -> colors.info
                                    else -> colors.textSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (!layoutPreferences.isMinimalistMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!layoutPreferences.isMinimalistMode) {
                            val isLastSplit = currentRun.segments.let { currentIndex == it.size - 1 }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { /* Consume click */ }
                            ) {
                                TimerControls(
                                    timerState = timerState,
                                    isLastSplit = isLastSplit,
                                    onStartSplit = {
                                        if (timerState is TimerState.Idle) {
                                            viewModel.startTimer()
                                        } else {
                                            viewModel.split()
                                        }
                                    },
                                    onPauseResume = { viewModel.pauseResume() },
                                    onUndo = { viewModel.undoSplit() },
                                    onSkip = { viewModel.skipSplit() },
                                    onReset = { viewModel.reset(saveAttempt = true) },
                                    showUndo = layoutPreferences.showUndoButton,
                                    showSkip = layoutPreferences.showSkipButton,
                                    showPause = layoutPreferences.showPauseButton
                                )
                            }
                        }
                    }
                }
            } else {
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (layoutPreferences.isMinimalistMode) 16.dp else 64.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSplitsInFullscreen) {
                        Box(modifier = Modifier.weight(1.2f)) {
                        SplitList(
                            run = currentRun,
                            currentSegmentIndex = currentIndex,
                            splitTimes = splitTimes,
                            comparisonName = activeComp,
                            timingMethod = TimingMethod.REAL_TIME,
                            timeFormat = layoutPreferences.timeFormat,
                            layoutPreferences = layoutPreferences,
                            modifier = Modifier.fillMaxSize(),
                            completedSplitsVisible = 2,
                            currentElapsed = currentElapsed,
                            activeSegmentDelta = currentDelta,
                            isTimerRunning = timerState is TimerState.Running
                        )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (!layoutPreferences.isMinimalistMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${currentRun.gameInfo.gameName} — ${currentRun.gameInfo.categoryName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = currentElapsed.formatted(layoutPreferences.timeFormat),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = if (showSplitsInFullscreen) 70.sp else 110.sp,
                                lineHeight = if (showSplitsInFullscreen) 77.sp else 121.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = timerColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            softWrap = true
                        )

                        if (!layoutPreferences.isMinimalistMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val stateText = when (timerState) {
                                is TimerState.Running -> stringResource(R.string.phase_running)
                                is TimerState.Paused -> stringResource(R.string.phase_paused)
                                is TimerState.Finished -> stringResource(R.string.phase_ended)
                                else -> stringResource(R.string.phase_not_running)
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (timerState) {
                                    is TimerState.Running -> colors.success.copy(alpha = 0.2f)
                                    is TimerState.Paused -> colors.warning.copy(alpha = 0.2f)
                                    is TimerState.Finished -> colors.info.copy(alpha = 0.2f)
                                    else -> colors.textDisabled.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = stateText.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (timerState) {
                                        is TimerState.Running -> colors.success
                                        is TimerState.Paused -> colors.warning
                                        is TimerState.Finished -> colors.info
                                        else -> colors.textSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (!layoutPreferences.isMinimalistMode) {
                            val isLastSplit = currentRun.segments.let { currentIndex == it.size - 1 }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { /* Consume click */ }
                            ) {
                                TimerControls(
                                    timerState = timerState,
                                    isLastSplit = isLastSplit,
                                    onStartSplit = {
                                        if (timerState is TimerState.Idle) {
                                            viewModel.startTimer()
                                        } else {
                                            viewModel.split()
                                        }
                                    },
                                    onPauseResume = { viewModel.pauseResume() },
                                    onUndo = { viewModel.undoSplit() },
                                    onSkip = { viewModel.skipSplit() },
                                    onReset = { viewModel.reset(saveAttempt = true) },
                                    showUndo = layoutPreferences.showUndoButton,
                                    showSkip = layoutPreferences.showSkipButton,
                                    showPause = layoutPreferences.showPauseButton
                                )
                            }
                        }
                    }
                }
            }

            
            val showFullscreenControls = !layoutPreferences.isMinimalistMode || timerState !is TimerState.Running

            if (showFullscreenControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { /* Consume click */ },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSplitsInFullscreen = !showSplitsInFullscreen },
                        modifier = Modifier.background(
                            if (showSplitsInFullscreen) colors.success.copy(alpha = 0.2f)
                            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Toggle Splits",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Fullscreen",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize().then(backgroundModifier),
            topBar = {
                if (!layoutPreferences.timerLocked) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.timer_title), color = colors.textPrimary) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = colors.textPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                viewModel.exportCurrentRun(
                                    onSuccess = { bytes ->
                                        val filename = "${run?.gameInfo?.gameName ?: "splits"}.lss"
                                        try {
                                            val cacheFile = java.io.File(context.cacheDir, filename)
                                            cacheFile.writeBytes(bytes)
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                cacheFile
                                            )
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/octet-stream"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.timer_export_chooser_title)))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, context.getString(R.string.timer_export_error, e.localizedMessage), android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        android.widget.Toast.makeText(context, context.getString(R.string.timer_export_error, error.localizedMessage), android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.timer_desc_share),
                                    tint = colors.textPrimary
                                )
                            }
                            IconButton(onClick = onEditLayout) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = stringResource(R.string.layout_editor_title),
                                    tint = colors.textPrimary
                                )
                            }
                            IconButton(onClick = { onEditSplits(runId) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.timer_edit_splits),
                                    tint = colors.textPrimary
                                )
                            }
                            IconButton(onClick = { onNavigateToStats(runId) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = stringResource(R.string.timer_desc_stats),
                                    tint = colors.textPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            val currentRun = run
            if (currentRun == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val splitTimes = when (val state = timerState) {
                        is TimerState.Running -> state.splitTimes
                        is TimerState.Paused -> state.splitTimes
                        is TimerState.Finished -> state.splitTimes
                        else -> List(currentRun.segments.size) { null }
                    }

                    val currentIndex = when (val state = timerState) {
                        is TimerState.Running -> state.currentSegmentIndex
                        is TimerState.Paused -> state.currentSegmentIndex
                        is TimerState.Finished -> currentRun.segments.size
                        else -> 0
                    }

                    val activeComp = when (val state = timerState) {
                        is TimerState.Running -> state.comparison.name
                        is TimerState.Paused -> state.comparison.name
                        is TimerState.Finished -> state.comparison.name
                        else -> "Personal Best"
                    }

                    RunHeader(
                        run = currentRun,
                        activeComparison = activeComp,
                        onComparisonClick = { viewModel.cycleComparison() },
                        backgroundColor = if (layoutPreferences.backgroundGradientEnabled) androidx.compose.ui.graphics.Color.Transparent else colors.elevatedSurface
                    )

                    val isLastSplit = currentRun.segments.let { currentIndex == it.size - 1 }
                    val currentDelta = computeCurrentDelta(
                        timerState = timerState,
                        currentRun = currentRun,
                        currentIndex = currentIndex,
                        currentElapsed = currentElapsed,
                        activeComp = activeComp
                    )

                    val sob = currentRun.sumOfBest

                    if (layoutPreferences.showSplits) {
                        SplitList(
                            run = currentRun,
                            currentSegmentIndex = currentIndex,
                            splitTimes = splitTimes,
                            comparisonName = activeComp,
                            timingMethod = TimingMethod.REAL_TIME,
                            timeFormat = layoutPreferences.timeFormat,
                            layoutPreferences = layoutPreferences,
                            modifier = Modifier.weight(1f),
                            currentElapsed = currentElapsed,
                            activeSegmentDelta = currentDelta,
                            isTimerRunning = timerState is TimerState.Running
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }


                    TimerDisplay(
                        elapsedTime = currentElapsed,
                        delta = currentDelta,
                        timerState = timerState,
                        timeFormat = layoutPreferences.timeFormat,
                        colorMode = layoutPreferences.colorMode,
                        layoutPreferences = layoutPreferences,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    isFullscreen = true
                                }
                            )
                        }
                    )

                    if (layoutPreferences.showSumOfBest) {
                        sob?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.timer_sum_of_best),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textTertiary
                                )
                                Text(
                                    text = it.formatted(layoutPreferences.timeFormat),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    
                    TimerControls(
                        timerState = timerState,
                        isLastSplit = isLastSplit,
                        onStartSplit = {
                            if (timerState is TimerState.Idle) {
                                viewModel.startTimer()
                            } else {
                                viewModel.split()
                            }
                        },
                        onPauseResume = { viewModel.pauseResume() },
                        onUndo = { viewModel.undoSplit() },
                        onSkip = { viewModel.skipSplit() },
                        onReset = { viewModel.reset(saveAttempt = true) },
                        showUndo = layoutPreferences.showUndoButton,
                        showSkip = layoutPreferences.showSkipButton,
                        showPause = layoutPreferences.showPauseButton
                    )
                }
            }
        }
    }
}
