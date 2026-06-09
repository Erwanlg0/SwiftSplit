package com.elg.speedruncompanion.ui.screen.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.speedruncompanion.domain.model.ComparisonName
import com.elg.speedruncompanion.domain.model.Delta
import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.TimerState
import com.elg.speedruncompanion.domain.model.TimingMethod
import com.elg.speedruncompanion.domain.model.TimeSpan
import com.elg.speedruncompanion.domain.service.SplitTimeCalculator
import com.elg.speedruncompanion.ui.screen.layout.toComposeColor
import com.elg.speedruncompanion.domain.service.TimerDisplayColorResolver
import com.elg.speedruncompanion.ui.screen.timer.components.RunHeader
import com.elg.speedruncompanion.ui.screen.timer.components.SplitList
import com.elg.speedruncompanion.ui.screen.timer.components.TimerControls
import com.elg.speedruncompanion.ui.screen.timer.components.TimerDisplay
import com.elg.speedruncompanion.ui.theme.SpeedrunThemeColors

import androidx.compose.ui.res.stringResource
import com.elg.speedruncompanion.R

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
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
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val run by viewModel.run.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val currentElapsed by viewModel.currentElapsed.collectAsState()
    val layoutPreferences by viewModel.layoutPreferences.collectAsState()

    val colors = SpeedrunThemeColors.colors
    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    // Keep screen wake lock active when running
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

    // Manage screen orientation and system bars visibility based on fullscreen state
    DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.pauseResume()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Game Name & Category
                Text(
                    text = "${currentRun.gameInfo.gameName} — ${currentRun.gameInfo.categoryName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Determine delta color indicator for the huge text
                val currentIndex = when (val state = timerState) {
                    is TimerState.Running -> state.currentSegmentIndex
                    is TimerState.Paused -> state.currentSegmentIndex
                    else -> 0
                }
                val activeComp = when (val state = timerState) {
                    is TimerState.Running -> state.comparison.name
                    is TimerState.Paused -> state.comparison.name
                    is TimerState.Finished -> state.comparison.name
                    else -> "Personal Best"
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
                ).toComposeColor(colors)

                Text(
                    text = currentElapsed.formatted(layoutPreferences.timeFormat),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp),
                    fontWeight = FontWeight.Black,
                    color = timerColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

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

                // Current Segment Name or Completed indicator
                val currentSegmentName = if (timerState is TimerState.Finished) {
                    stringResource(R.string.timer_finished)
                } else if (currentIndex < currentRun.segments.size) {
                    currentRun.segments[currentIndex].name
                } else {
                    ""
                }
                if (currentSegmentName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentSegmentName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Exit button top-right
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Fullscreen",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.timer_title), color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = colors.textPrimary
                            )
                        }
                    },
                    actions = {
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.deepBackground
                    )
                )
            },
            containerColor = colors.deepBackground
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
                    // Header
                    RunHeader(run = currentRun)

                    // Splits list
                    val splitTimes = when (val state = timerState) {
                        is TimerState.Running -> state.splitTimes
                        is TimerState.Paused -> state.splitTimes
                        is TimerState.Finished -> state.splitTimes
                        else -> List(currentRun.segments.size) { null }
                    }

                    val currentIndex = when (val state = timerState) {
                        is TimerState.Running -> state.currentSegmentIndex
                        is TimerState.Paused -> state.currentSegmentIndex
                        else -> 0
                    }

                    val activeComp = when (val state = timerState) {
                        is TimerState.Running -> state.comparison.name
                        is TimerState.Paused -> state.comparison.name
                        is TimerState.Finished -> state.comparison.name
                        else -> "Personal Best"
                    }

                    val isLastSplit = currentRun.segments.let { currentIndex == it.size - 1 }

                    SplitList(
                        run = currentRun,
                        currentSegmentIndex = currentIndex,
                        splitTimes = splitTimes,
                        comparisonName = activeComp,
                        timingMethod = TimingMethod.REAL_TIME,
                        modifier = Modifier.weight(1f)
                    )

                    val currentDelta = computeCurrentDelta(
                        timerState = timerState,
                        currentRun = currentRun,
                        currentIndex = currentIndex,
                        currentElapsed = currentElapsed,
                        activeComp = activeComp
                    )

                    TimerDisplay(
                        elapsedTime = currentElapsed,
                        delta = currentDelta,
                        timerState = timerState,
                        timeFormat = layoutPreferences.timeFormat,
                        colorMode = layoutPreferences.colorMode,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    isFullscreen = true
                                }
                            )
                        }
                    )

                    // Controls
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
                        onReset = { viewModel.reset(saveAttempt = true) }
                    )
                }
            }
        }
    }
}
