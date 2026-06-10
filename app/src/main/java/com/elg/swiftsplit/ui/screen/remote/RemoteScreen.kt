package com.elg.swiftsplit.ui.screen.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.ui.screen.timer.components.SplitList
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.Segment
import com.elg.swiftsplit.domain.model.GameInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R

import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.withFrameMillis
import android.view.WindowManager
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.service.TimerDisplayColorResolver
import com.elg.swiftsplit.ui.screen.layout.toComposeColorWithPrefs
import com.elg.swiftsplit.ui.screen.timer.components.TimerControls
import androidx.compose.runtime.DisposableEffect

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun rememberAnimatedRemoteTime(
    rawTimeStr: String,
    phase: String,
    formatOptions: TimeFormatOptions
): String {
    val parsedRaw = remember(rawTimeStr) {
        TimeSpan.fromTimeString(rawTimeStr) ?: TimeSpan.ZERO
    }
    
    val formattedRaw = remember(parsedRaw, formatOptions) {
        parsedRaw.formatted(formatOptions)
    }

    if (phase != "Running") {
        return formattedRaw
    }

    val latestRawTime = rememberUpdatedState(rawTimeStr)
    var displayTimeStr by remember { mutableStateOf(formattedRaw) }

    LaunchedEffect(phase) {
        if (phase == "Running") {
            var remoteRunStartPhoneTime = -1L

            while (true) {
                withFrameMillis { frameTime ->
                    val now = System.currentTimeMillis()
                    val rawTimeSpan = TimeSpan.fromTimeString(latestRawTime.value) ?: TimeSpan.ZERO
                    val rawTimeMs = rawTimeSpan.totalMilliseconds

                    if (remoteRunStartPhoneTime == -1L || kotlin.math.abs((now - remoteRunStartPhoneTime) - rawTimeMs) > 200) {
                        remoteRunStartPhoneTime = now - rawTimeMs
                    } else {
                        val targetStart = now - rawTimeMs
                        remoteRunStartPhoneTime = (remoteRunStartPhoneTime * 0.95 + targetStart * 0.05).toLong()
                    }

                    val currentElapsed = now - remoteRunStartPhoneTime
                    val interpolatedTime = TimeSpan(if (currentElapsed > 0) currentElapsed else 0L)
                    displayTimeStr = interpolatedTime.formatted(formatOptions)
                }
            }
        }
    }

    LaunchedEffect(formattedRaw) {
        displayTimeStr = formattedRaw
    }

    return displayTimeStr
}

@Composable
private fun getTranslatedPhase(phase: String): String {
    return when (phase.trim()) {
        "Running" -> stringResource(R.string.phase_running)
        "Paused" -> stringResource(R.string.phase_paused)
        "Ended" -> stringResource(R.string.phase_ended)
        else -> stringResource(R.string.phase_not_running)
    }
}

internal fun resolveRemoteTapCommand(phase: String): String? = when {
    phase.equals("NotRunning", ignoreCase = true) -> "startorsplit"
    phase.equals("Paused", ignoreCase = true) -> "resume"
    phase.equals("Running", ignoreCase = true) -> "pause"
    else -> null
}

internal fun resolveRemotePauseResumeCommand(phase: String): String? = when {
    phase.equals("Paused", ignoreCase = true) -> "resume"
    phase.equals("Running", ignoreCase = true) -> "pause"
    else -> null
}

internal fun parseRemoteDelta(deltaStr: String?): Delta? {
    if (deltaStr.isNullOrBlank()) return null
    val trimmed = deltaStr.trim()
    val isAhead = trimmed.startsWith("-")
    val numericPart = trimmed.removePrefix("+").removePrefix("-").trim()
    val time = TimeSpan.fromTimeString(numericPart) ?: return null
    val signedTime = if (isAhead) -time else time
    val status = if (isAhead) Delta.Status.AHEAD_GAINING else Delta.Status.BEHIND_LOSING
    return Delta(signedTime, status)
}

internal fun mapRemotePhaseToTimerState(
    phase: String,
    splitIndex: Int,
    splitTimes: List<TimeSpan?>,
    elapsed: TimeSpan
): TimerState {
    val comparison = ComparisonName.PERSONAL_BEST
    val index = splitIndex.coerceAtLeast(0)
    return when {
        phase.equals("Running", ignoreCase = true) -> TimerState.Running(
            startTime = 0L,
            pauseAccumulator = 0L,
            currentSegmentIndex = index,
            splitTimes = splitTimes,
            comparison = comparison
        )
        phase.equals("Paused", ignoreCase = true) -> TimerState.Paused(
            elapsedTime = elapsed,
            currentSegmentIndex = index,
            splitTimes = splitTimes,
            comparison = comparison
        )
        phase.equals("Ended", ignoreCase = true) -> TimerState.Finished(
            finalTime = elapsed,
            splitTimes = splitTimes,
            comparison = comparison
        )
        else -> TimerState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLayoutEditor: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val host by viewModel.host.collectAsStateWithLifecycle()
    val port by viewModel.port.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastResponse by viewModel.lastResponse.collectAsStateWithLifecycle()
    val remoteTime by viewModel.remoteTime.collectAsStateWithLifecycle()
    val remotePhase by viewModel.remotePhase.collectAsStateWithLifecycle()
    val remoteSplitName by viewModel.remoteSplitName.collectAsStateWithLifecycle()
    val remoteSplitIndex by viewModel.remoteSplitIndex.collectAsStateWithLifecycle()
    val remoteDelta by viewModel.remoteDelta.collectAsStateWithLifecycle()
    val remoteGameName by viewModel.remoteGameName.collectAsStateWithLifecycle()
    val remoteCategoryName by viewModel.remoteCategoryName.collectAsStateWithLifecycle()
    val remoteSplits by viewModel.remoteSplits.collectAsStateWithLifecycle()
    val remoteSplitTimes by viewModel.remoteSplitTimes.collectAsStateWithLifecycle()
    val layoutPrefs by viewModel.timerLayoutPreferences.collectAsStateWithLifecycle()
    val smoothRemoteTime = rememberAnimatedRemoteTime(remoteTime, remotePhase, layoutPrefs.timeFormat)
    val currentElapsed = remember(smoothRemoteTime) {
        TimeSpan.fromTimeString(smoothRemoteTime) ?: TimeSpan.ZERO
    }
    val currentDelta = remember(remoteDelta) { parseRemoteDelta(remoteDelta) }
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    var showSplitsInFullscreen by rememberSaveable(layoutPrefs.showSplits) {
        mutableStateOf(layoutPrefs.showSplits)
    }
    val mockRun = remember(remoteSplits, remoteGameName, remoteCategoryName) {
        Run(
            gameInfo = GameInfo(
                gameName = remoteGameName,
                categoryName = remoteCategoryName
            ),
            segments = remoteSplits.map { name ->
                Segment(name = name)
            }
        )
    }
    val colors = SwiftSplitThemeColors.colors
    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var isFormatExpanded by rememberSaveable { mutableStateOf(false) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            isSettingsExpanded = false
        } else if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
            isSettingsExpanded = true
        }
    }

    
    LaunchedEffect(lastResponse) {
        if (!lastResponse.isNullOrBlank()) {
            android.widget.Toast.makeText(context, context.getString(R.string.remote_toast_response, lastResponse), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(remotePhase) {
        val activity = context.findActivity()
        if (remotePhase.equals("Running", ignoreCase = true)) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    
    androidx.compose.runtime.DisposableEffect(isFullscreen, layoutPrefs.fullscreenOrientation) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                val req = when (layoutPrefs.fullscreenOrientation) {
                    com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                activity.requestedOrientation = req
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

    
    if (showTutorial) {
        var currentStep by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            title = {
                Text(
                    text = stringResource(R.string.remote_tuto_title, currentStep),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentStep == 1) {
                        Image(
                            painter = painterResource(id = R.drawable.tuto_1),
                            contentDescription = "Étape 1",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.Black.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
                        )
                        Text(
                            text = stringResource(R.string.remote_tuto_step_1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "https://livesplit.org/downloads/",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable {
                                    try {
                                        uriHandler.openUri("https://livesplit.org/downloads/")
                                    } catch (e: Exception) {
                                        
                                    }
                                }
                                .padding(8.dp)
                        )
                        Text(
                            text = stringResource(R.string.remote_tuto_step_1_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    } else if (currentStep == 6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(colors.elevatedSurface.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colors.success
                            )
                        }
                        Text(
                            text = stringResource(R.string.remote_tuto_step_6),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val stepDesc = when (currentStep) {
                            2 -> stringResource(R.string.remote_tuto_step_2)
                            3 -> stringResource(R.string.remote_tuto_step_3)
                            4 -> stringResource(R.string.remote_tuto_step_4)
                            else -> stringResource(R.string.remote_tuto_step_5)
                        }
                        val imageRes = when (currentStep) {
                            2 -> R.drawable.tuto_2
                            3 -> R.drawable.tuto_3
                            4 -> R.drawable.tuto_4
                            else -> R.drawable.tuto_5
                        }

                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Étape $currentStep",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.Black.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
                        )

                        Text(
                            text = stepDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentStep < 6) {
                            currentStep++
                        } else {
                            showTutorial = false
                        }
                    }
                ) {
                    Text(if (currentStep < 6) stringResource(R.string.remote_tuto_btn_next) else stringResource(R.string.remote_tuto_btn_finish))
                }
            },
            dismissButton = {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep-- }) {
                        Text(stringResource(R.string.remote_tuto_btn_prev), color = colors.textSecondary)
                    }
                } else {
                    TextButton(onClick = { showTutorial = false }) {
                        Text(stringResource(R.string.remote_tuto_btn_close), color = colors.textSecondary)
                    }
                }
            },
            containerColor = colors.elevatedSurface
        )
    }

    if (isFullscreen) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isPortrait = when (layoutPrefs.fullscreenOrientation) {
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.PORTRAIT -> true
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.LANDSCAPE -> false
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.AUTO ->
                configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }
        val currentIndex = if (remotePhase.equals("Ended", ignoreCase = true)) {
            mockRun.segments.size
        } else {
            remoteSplitIndex.coerceAtLeast(0)
        }
        val remoteTimerState = remember(remotePhase, remoteSplitIndex, remoteSplitTimes, currentElapsed) {
            mapRemotePhaseToTimerState(remotePhase, remoteSplitIndex, remoteSplitTimes, currentElapsed)
        }
        val timerColor = TimerDisplayColorResolver.resolve(
            colorMode = layoutPrefs.colorMode,
            timerState = remoteTimerState,
            delta = currentDelta
        ).toComposeColorWithPrefs(colors, layoutPrefs)
        val isLastSplit = mockRun.segments.isNotEmpty() && currentIndex == mockRun.segments.lastIndex
        val gameHeader = when {
            remoteGameName.isNotBlank() && remoteCategoryName.isNotBlank() -> "$remoteGameName — $remoteCategoryName"
            remoteGameName.isNotBlank() -> remoteGameName
            remoteCategoryName.isNotBlank() -> remoteCategoryName
            else -> ""
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    resolveRemoteTapCommand(remotePhase)?.let { viewModel.sendCommand(it) }
                }
        ) {
            if (isPortrait) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    if (gameHeader.isNotEmpty()) {
                        Text(
                            text = gameHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    if (showSplitsInFullscreen && remoteSplits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SplitList(
                            run = mockRun,
                            currentSegmentIndex = currentIndex,
                            splitTimes = remoteSplitTimes,
                            comparisonName = "Personal Best",
                            timingMethod = TimingMethod.REAL_TIME,
                            timeFormat = layoutPrefs.timeFormat,
                            layoutPreferences = layoutPrefs,
                            modifier = Modifier.weight(1f),
                            completedSplitsVisible = 2,
                            currentElapsed = currentElapsed,
                            activeSegmentDelta = currentDelta,
                            isTimerRunning = remotePhase.equals("Running", ignoreCase = true)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = smoothRemoteTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (showSplitsInFullscreen && remoteSplits.isNotEmpty()) 70.sp else 96.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = timerColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (remotePhase) {
                            "Running" -> colors.success.copy(alpha = 0.2f)
                            "Paused" -> colors.warning.copy(alpha = 0.2f)
                            "Ended" -> colors.info.copy(alpha = 0.2f)
                            else -> colors.textDisabled.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = getTranslatedPhase(remotePhase).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (remotePhase) {
                                "Running" -> colors.success
                                "Paused" -> colors.warning
                                "Ended" -> colors.info
                                else -> colors.textSecondary
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    val currentSegmentName = when {
                        remotePhase.equals("Ended", ignoreCase = true) -> stringResource(R.string.timer_finished)
                        currentIndex < mockRun.segments.size -> mockRun.segments[currentIndex].name
                        !remoteSplitName.isNullOrBlank() -> remoteSplitName!!
                        else -> ""
                    }
                    if (currentSegmentName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentSegmentName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { }
                    ) {
                        TimerControls(
                            timerState = remoteTimerState,
                            isLastSplit = isLastSplit,
                            onStartSplit = {
                                if (remoteTimerState is TimerState.Idle) {
                                    viewModel.sendCommand("startorsplit")
                                } else {
                                    viewModel.sendCommand("split")
                                }
                            },
                            onPauseResume = {
                                resolveRemotePauseResumeCommand(remotePhase)?.let { viewModel.sendCommand(it) }
                            },
                            onUndo = { viewModel.sendCommand("unsplit") },
                            onSkip = { viewModel.sendCommand("skipsplit") },
                            onReset = { viewModel.sendCommand("reset") }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSplitsInFullscreen && remoteSplits.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            SplitList(
                                run = mockRun,
                                currentSegmentIndex = currentIndex,
                                splitTimes = remoteSplitTimes,
                                comparisonName = "Personal Best",
                                timingMethod = TimingMethod.REAL_TIME,
                                timeFormat = layoutPrefs.timeFormat,
                                layoutPreferences = layoutPrefs,
                                modifier = Modifier.fillMaxSize(),
                                completedSplitsVisible = 2,
                                currentElapsed = currentElapsed,
                                activeSegmentDelta = currentDelta,
                                isTimerRunning = remotePhase.equals("Running", ignoreCase = true)
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
                        if (gameHeader.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gameHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = smoothRemoteTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = if (showSplitsInFullscreen && remoteSplits.isNotEmpty()) 56.sp else 80.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = timerColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (remotePhase) {
                                "Running" -> colors.success.copy(alpha = 0.2f)
                                "Paused" -> colors.warning.copy(alpha = 0.2f)
                                "Ended" -> colors.info.copy(alpha = 0.2f)
                                else -> colors.textDisabled.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = getTranslatedPhase(remotePhase).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (remotePhase) {
                                    "Running" -> colors.success
                                    "Paused" -> colors.warning
                                    "Ended" -> colors.info
                                    else -> colors.textSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        val currentSegmentName = when {
                            remotePhase.equals("Ended", ignoreCase = true) -> stringResource(R.string.timer_finished)
                            currentIndex < mockRun.segments.size -> mockRun.segments[currentIndex].name
                            !remoteSplitName.isNullOrBlank() -> remoteSplitName!!
                            else -> ""
                        }
                        if (currentSegmentName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentSegmentName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { }
                        ) {
                            TimerControls(
                                timerState = remoteTimerState,
                                isLastSplit = isLastSplit,
                                onStartSplit = {
                                    if (remoteTimerState is TimerState.Idle) {
                                        viewModel.sendCommand("startorsplit")
                                    } else {
                                        viewModel.sendCommand("split")
                                    }
                                },
                                onPauseResume = {
                                    resolveRemotePauseResumeCommand(remotePhase)?.let { viewModel.sendCommand(it) }
                                },
                                onUndo = { viewModel.sendCommand("unsplit") },
                                onSkip = { viewModel.sendCommand("skipsplit") },
                                onReset = { viewModel.sendCommand("reset") }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showSplitsInFullscreen = !showSplitsInFullscreen },
                    modifier = Modifier.background(
                        if (showSplitsInFullscreen) colors.success.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Toggle Splits",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.remote_title), color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Tuto de connexion", tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.deepBackground
                    )
                )
            },
            containerColor = colors.deepBackground
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            if (connectionState == ConnectionState.CONNECTED) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.error.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = colors.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSettingsExpanded = !isSettingsExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.remote_conn_settings),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isSettingsExpanded) "Collapse" else "Expand",
                                        tint = colors.textSecondary
                                    )
                                }

                                val statusColor = colors.success

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = statusColor
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = connectionState.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isSettingsExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = host,
                                            onValueChange = { viewModel.updateHost(it) },
                                            label = { Text(stringResource(R.string.remote_ip_address)) },
                                            modifier = Modifier.weight(1f),
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textSecondary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = port,
                                            onValueChange = { viewModel.updatePort(it) },
                                            label = { Text(stringResource(R.string.remote_port)) },
                                            modifier = Modifier.width(100.dp),
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textSecondary
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.disconnect() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.error
                                        )
                                    ) {
                                        Text(stringResource(R.string.remote_disconnect))
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFormatExpanded = !isFormatExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.remote_format_header),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isFormatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isFormatExpanded) "Collapse" else "Expand",
                                        tint = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToLayoutEditor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit Full Layout",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            var showPatternMenu by remember { mutableStateOf(false) }
                            val currentPattern = layoutPrefs.timeFormat.pattern

                            val sampleShort = TimeSpan.fromSeconds(1.23)
                            val sampleLong = TimeSpan.fromHours(1.0) + TimeSpan.fromMinutes(5.0) + TimeSpan.fromSeconds(30.45)

                            fun getPatternDisplayName(pat: com.elg.swiftsplit.domain.model.TimeFormatPattern): String = when (pat) {
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS_SS -> "HH:mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS_S -> "HH:mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS -> "HH:mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS_SS -> "[HH:]mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS_S -> "[HH:]mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS -> "[HH:]mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_SS -> "[HH:][mm:]ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> "[HH:][mm:]ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS_SS -> "mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS_S -> "mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS -> "mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS_SS -> "[mm:]ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS_S -> "[mm:]ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS -> "[mm:]ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS_SS -> "ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS_S -> "ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS -> "ss"
                            }

                            AnimatedVisibility(visible = isFormatExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ListItem(
                                            headlineContent = { Text(stringResource(R.string.remote_format_select)) },
                                            supportingContent = {
                                                Text(
                                                    text = getPatternDisplayName(currentPattern) + "\n" + stringResource(R.string.layout_editor_format_examples, sampleShort.formatted(layoutPrefs.timeFormat), sampleLong.formatted(layoutPrefs.timeFormat)),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            modifier = Modifier.clickable { showPatternMenu = true },
                                            colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                                        )
                                        DropdownMenu(
                                            expanded = showPatternMenu,
                                            onDismissRequest = { showPatternMenu = false }
                                        ) {
                                            com.elg.swiftsplit.domain.model.TimeFormatPattern.entries.forEach { pat ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(getPatternDisplayName(pat), fontWeight = FontWeight.Bold)
                                                            Text(
                                                                text = stringResource(R.string.layout_editor_format_examples, sampleShort.formatted(TimeFormatOptions(pattern = pat)), sampleLong.formatted(TimeFormatOptions(pattern = pat))),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = colors.textSecondary
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
                            }
                        }
                    }

                    val currentIndex = if (remotePhase.equals("Ended", ignoreCase = true)) {
                        mockRun.segments.size
                    } else {
                        remoteSplitIndex.coerceAtLeast(0)
                    }

                    if (remoteSplits.isNotEmpty()) {
                        SplitList(
                            run = mockRun,
                            currentSegmentIndex = currentIndex,
                            splitTimes = remoteSplitTimes,
                            comparisonName = "Personal Best",
                            timingMethod = TimingMethod.REAL_TIME,
                            timeFormat = layoutPrefs.timeFormat,
                            layoutPreferences = layoutPrefs,
                            modifier = Modifier.weight(1f),
                            completedSplitsVisible = 2,
                            currentElapsed = currentElapsed,
                            activeSegmentDelta = currentDelta,
                            isTimerRunning = remotePhase.equals("Running", ignoreCase = true)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        isFullscreen = true
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.remote_live_timer),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = smoothRemoteTime,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (remotePhase) {
                                    "Running" -> colors.timerText
                                    "Paused" -> colors.warning
                                    "Ended" -> colors.success
                                    else -> colors.textTertiary
                                },
                                textAlign = TextAlign.Center
                            )

                            if (!remoteSplitName.isNullOrBlank() || remoteSplitIndex >= 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${remoteSplitIndex + 1}. ${remoteSplitName ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    val deltaStr = remoteDelta ?: ""
                                    if (deltaStr.isNotBlank()) {
                                        val isAhead = deltaStr.startsWith("-")
                                        val deltaColor = if (isAhead) colors.aheadGaining else colors.behindLosing
                                        Text(
                                            text = deltaStr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = deltaColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (remotePhase) {
                                    "Running" -> colors.success.copy(alpha = 0.15f)
                                    "Paused" -> colors.warning.copy(alpha = 0.15f)
                                    "Ended" -> colors.info.copy(alpha = 0.15f)
                                    else -> colors.textDisabled.copy(alpha = 0.15f)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = getTranslatedPhase(remotePhase),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (remotePhase) {
                                        "Running" -> colors.success
                                        "Paused" -> colors.warning
                                        "Ended" -> colors.info
                                        else -> colors.textSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.remote_controls),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.sendCommand("startorsplit") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.remote_btn_split), style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.sendCommand("split") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.info),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.timer_control_finish), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isPaused = remotePhase.equals("Paused", ignoreCase = true)
                            val pauseResumeCommand = resolveRemotePauseResumeCommand(remotePhase)
                            Button(
                                onClick = { pauseResumeCommand?.let { viewModel.sendCommand(it) } },
                                enabled = pauseResumeCommand != null,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.textSecondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(
                                        if (isPaused) R.string.remote_btn_resume else R.string.remote_btn_pause
                                    ),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = { viewModel.sendCommand("unsplit") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.remote_btn_undo), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.sendCommand("skipsplit") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textSecondary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.remote_btn_skip), style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.sendCommand("reset") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.remote_btn_reset), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = { viewModel.sendCommand("ping") },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.elevatedSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CastConnected, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.remote_btn_ping), color = colors.textPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.error.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = colors.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSettingsExpanded = !isSettingsExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.remote_conn_settings),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isSettingsExpanded) "Collapse" else "Expand",
                                        tint = colors.textSecondary
                                    )
                                }

                                val statusColor = when (connectionState) {
                                    ConnectionState.CONNECTED -> colors.success
                                    ConnectionState.CONNECTING -> colors.warning
                                    ConnectionState.ERROR -> colors.error
                                    ConnectionState.DISCONNECTED -> colors.textDisabled
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = statusColor
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = connectionState.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isSettingsExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = host,
                                            onValueChange = { viewModel.updateHost(it) },
                                            label = { Text(stringResource(R.string.remote_ip_address)) },
                                            modifier = Modifier.weight(1f),
                                            enabled = connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textSecondary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = port,
                                            onValueChange = { viewModel.updatePort(it) },
                                            label = { Text(stringResource(R.string.remote_port)) },
                                            modifier = Modifier.width(100.dp),
                                            enabled = connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textSecondary
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (connectionState == ConnectionState.CONNECTED) {
                                                viewModel.disconnect()
                                            } else {
                                                viewModel.connect()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (connectionState == ConnectionState.CONNECTED) colors.error else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(if (connectionState == ConnectionState.CONNECTED) stringResource(R.string.remote_disconnect) else stringResource(R.string.remote_connect))
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFormatExpanded = !isFormatExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.remote_format_header),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isFormatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isFormatExpanded) "Collapse" else "Expand",
                                        tint = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToLayoutEditor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit Full Layout",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            var showPatternMenu by remember { mutableStateOf(false) }
                            val currentPattern = layoutPrefs.timeFormat.pattern

                            val sampleShort = TimeSpan.fromSeconds(1.23)
                            val sampleLong = TimeSpan.fromHours(1.0) + TimeSpan.fromMinutes(5.0) + TimeSpan.fromSeconds(30.45)

                            fun getPatternDisplayName(pat: com.elg.swiftsplit.domain.model.TimeFormatPattern): String = when (pat) {
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS_SS -> "HH:mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS_S -> "HH:mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.HH_MM_SS -> "HH:mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS_SS -> "[HH:]mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS_S -> "[HH:]mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_MM_SS -> "[HH:]mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_SS -> "[HH:][mm:]ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_HH_OPT_MM_SS_S -> "[HH:][mm:]ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS_SS -> "mm:ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS_S -> "mm:ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.MM_SS -> "mm:ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS_SS -> "[mm:]ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS_S -> "[mm:]ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.OPT_MM_SS -> "[mm:]ss"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS_SS -> "ss.SS"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS_S -> "ss.S"
                                com.elg.swiftsplit.domain.model.TimeFormatPattern.SS -> "ss"
                            }

                            AnimatedVisibility(visible = isFormatExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ListItem(
                                            headlineContent = { Text(stringResource(R.string.remote_format_select)) },
                                            supportingContent = {
                                                Text(
                                                    text = getPatternDisplayName(currentPattern) + "\n" + stringResource(R.string.layout_editor_format_examples, sampleShort.formatted(layoutPrefs.timeFormat), sampleLong.formatted(layoutPrefs.timeFormat)),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            modifier = Modifier.clickable { showPatternMenu = true },
                                            colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                                        )
                                        DropdownMenu(
                                            expanded = showPatternMenu,
                                            onDismissRequest = { showPatternMenu = false }
                                        ) {
                                            com.elg.swiftsplit.domain.model.TimeFormatPattern.entries.forEach { pat ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(getPatternDisplayName(pat), fontWeight = FontWeight.Bold)
                                                            Text(
                                                                text = stringResource(R.string.layout_editor_format_examples, sampleShort.formatted(TimeFormatOptions(pattern = pat)), sampleLong.formatted(TimeFormatOptions(pattern = pat))),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = colors.textSecondary
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
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CastConnected,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colors.textDisabled
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.remote_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                stringResource(R.string.remote_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
