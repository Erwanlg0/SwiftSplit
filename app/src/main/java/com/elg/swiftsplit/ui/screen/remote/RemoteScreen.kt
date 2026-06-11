package com.elg.swiftsplit.ui.screen.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.ui.screen.timer.components.TimerControls
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.view.WindowManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.ui.screen.layout.toComposeColor
import com.elg.swiftsplit.R
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.domain.model.FullscreenOrientationPreset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalConfiguration

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
    val parsedRaw = remember(rawTimeStr) { TimeSpan.fromTimeString(rawTimeStr) ?: TimeSpan.ZERO }
    val formattedRaw = remember(parsedRaw, formatOptions) { parsedRaw.formatted(formatOptions) }

    if (phase != "Running") return formattedRaw

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

    LaunchedEffect(formattedRaw) { displayTimeStr = formattedRaw }
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

@Composable
private fun RemoteSplitRow(
    split: RemoteViewModel.RemoteSplit,
    isActive: Boolean,
    isCompleted: Boolean,
    layoutPreferences: TimerLayoutPreferences = TimerLayoutPreferences.DEFAULT,
    modifier: Modifier = Modifier
) {
    val colors = SwiftSplitThemeColors.colors
    val activeBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1D51A7),
            Color(0xFF1D51A7).copy(alpha = 0.2f)
        )
    )

    val deltaColor = when {
        split.delta?.startsWith("-") == true -> colors.aheadGaining
        split.delta?.startsWith("+") == true -> colors.behindLosing
        else -> colors.textSecondary
    }

    Column(modifier = modifier.fillMaxWidth().graphicsLayer(alpha = layoutPreferences.segmentOpacity)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) activeBrush else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = split.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color.White else colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = split.delta ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                fontWeight = FontWeight.Bold,
                color = deltaColor,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = split.actualTime ?: "-",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color.White else colors.textSecondary,
                modifier = Modifier.width(90.dp),
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(
            color = Color(0x1AFFFFFF),
            thickness = 0.5.dp
        )
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
    val remoteTime by viewModel.remoteTime.collectAsStateWithLifecycle()
    val remotePhase by viewModel.remotePhase.collectAsStateWithLifecycle()
    val gameName by viewModel.gameName.collectAsStateWithLifecycle()
    val categoryName by viewModel.categoryName.collectAsStateWithLifecycle()
    val layoutPrefs by viewModel.timerLayoutPreferences.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentSplitName by viewModel.currentSplitName.collectAsStateWithLifecycle()
    val currentSplitIndex by viewModel.currentSplitIndex.collectAsStateWithLifecycle()
    val splitsList by viewModel.splitsList.collectAsStateWithLifecycle()
    val isSplitsListSupported by viewModel.isSplitsListSupported.collectAsStateWithLifecycle()
    val isReconnecting by viewModel.isReconnecting.collectAsStateWithLifecycle()
    val reconnectAttempts by viewModel.reconnectAttempts.collectAsStateWithLifecycle()
    
    val smoothRemoteTime = rememberAnimatedRemoteTime(remoteTime, remotePhase, layoutPrefs.timeFormat)
    val colors = SwiftSplitThemeColors.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var showSplitsInFullscreen by rememberSaveable(layoutPrefs.showSplits) {
        mutableStateOf(layoutPrefs.showSplits)
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            isSettingsExpanded = false
        }
    }

    DisposableEffect(remotePhase) {
        val activity = context.findActivity()
        if (remotePhase == "Running") {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(isFullscreen, layoutPrefs.fullscreenOrientation) {
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
                            contentDescription = "Step 1",
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
                                        // Handle gracefully
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
                        val imageRes = when (currentStep) {
                            2 -> R.drawable.tuto_2
                            3 -> R.drawable.tuto_3
                            4 -> R.drawable.tuto_4
                            else -> R.drawable.tuto_5
                        }
                        val stepDesc = when (currentStep) {
                            2 -> stringResource(R.string.remote_tuto_step_2)
                            3 -> stringResource(R.string.remote_tuto_step_3)
                            4 -> stringResource(R.string.remote_tuto_step_4)
                            else -> stringResource(R.string.remote_tuto_step_5)
                        }

                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Step $currentStep",
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
                TextButton(
                    onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            showTutorial = false
                        }
                    }
                ) {
                    Text(
                        text = stringResource(if (currentStep > 1) R.string.remote_tuto_btn_prev else R.string.remote_tuto_btn_close),
                        color = colors.textSecondary
                    )
                }
            },
            containerColor = colors.elevatedSurface
        )
    }

    if (isFullscreen) {
        val configuration = LocalConfiguration.current
        val isPortrait = when (layoutPrefs.fullscreenOrientation) {
            FullscreenOrientationPreset.PORTRAIT -> true
            FullscreenOrientationPreset.LANDSCAPE -> false
            FullscreenOrientationPreset.AUTO ->
                configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(remotePhase, layoutPrefs.isMinimalistMode) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (offset.y < 50 * density) return@detectTapGestures
                            
                            when (remotePhase) {
                                "NotRunning" -> viewModel.sendCommand("startorsplit")
                                "Running" -> {
                                    if (layoutPrefs.isMinimalistMode) {
                                        viewModel.sendCommand("split")
                                    } else {
                                        viewModel.sendCommand(if (remotePhase == "Paused") "resume" else "pause")
                                    }
                                }
                                "Paused" -> viewModel.sendCommand("resume")
                                else -> {}
                            }
                        },
                        onLongPress = { offset ->
                            if (offset.y < 50 * density) return@detectTapGestures
                            if (layoutPrefs.isMinimalistMode && remotePhase == "Running") {
                                viewModel.sendCommand("pause")
                            }
                        }
                    )
                }
        ) {
            val currentElapsed = remember(smoothRemoteTime) { TimeSpan.fromTimeString(smoothRemoteTime) ?: TimeSpan.ZERO }
            val timerState = remember(remotePhase, currentElapsed) {
                when (remotePhase) {
                    "Running" -> TimerState.Running(0L, 0L, 0, emptyList(), ComparisonName.PERSONAL_BEST)
                    "Paused" -> TimerState.Paused(currentElapsed, 0, emptyList(), ComparisonName.PERSONAL_BEST)
                    "Ended" -> TimerState.Finished(currentElapsed, emptyList(), ComparisonName.PERSONAL_BEST)
                    else -> TimerState.Idle
                }
            }

            if (isPortrait) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (layoutPrefs.isMinimalistMode) 16.dp else 64.dp,
                            bottom = 24.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    if (!layoutPrefs.isMinimalistMode) {
                        Text(
                            text = if (!gameName.isNullOrBlank()) "$gameName — ${categoryName ?: ""}" else stringResource(R.string.remote_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    if (showSplitsInFullscreen) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isSplitsListSupported && splitsList.isNotEmpty()) {
                            val listState = rememberLazyListState()
                            val scrollTarget = (currentSplitIndex - 2).coerceAtLeast(0)
                            LaunchedEffect(scrollTarget) {
                                if (scrollTarget in splitsList.indices) {
                                    listState.animateScrollToItem(scrollTarget)
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(splitsList) { index, split ->
                                    RemoteSplitRow(
                                        split = split,
                                        isActive = index == currentSplitIndex,
                                        isCompleted = index < currentSplitIndex,
                                        layoutPreferences = layoutPrefs
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = smoothRemoteTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (showSplitsInFullscreen) 80.sp else 120.sp,
                            lineHeight = if (showSplitsInFullscreen) 88.sp else 132.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = colors.timerText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        softWrap = true
                    )

                    if (!layoutPrefs.isMinimalistMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when(remotePhase) {
                                "Running" -> colors.success.copy(alpha = 0.2f)
                                "Paused" -> colors.warning.copy(alpha = 0.2f)
                                "Ended" -> colors.info.copy(alpha = 0.2f)
                                else -> colors.textDisabled.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = getTranslatedPhase(remotePhase).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when(remotePhase) {
                                    "Running" -> colors.success
                                    "Paused" -> colors.warning
                                    "Ended" -> colors.info
                                    else -> colors.textSecondary
                                }
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
                                timerState = timerState,
                                isLastSplit = false,
                                onStartSplit = { viewModel.sendCommand("startorsplit") },
                                onPauseResume = { viewModel.sendCommand(if (remotePhase == "Paused") "resume" else "pause") },
                                onUndo = { viewModel.sendCommand("unsplit") },
                                onSkip = { viewModel.sendCommand("skipsplit") },
                                onReset = { viewModel.sendCommand("reset") },
                                showUndo = layoutPrefs.showUndoButton,
                                showSkip = layoutPrefs.showSkipButton,
                                showPause = layoutPrefs.showPauseButton
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (layoutPrefs.isMinimalistMode) 16.dp else 64.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSplitsInFullscreen) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            if (isSplitsListSupported && splitsList.isNotEmpty()) {
                                val listState = rememberLazyListState()
                                val scrollTarget = (currentSplitIndex - 2).coerceAtLeast(0)
                                LaunchedEffect(scrollTarget) {
                                    if (scrollTarget in splitsList.indices) {
                                        listState.animateScrollToItem(scrollTarget)
                                    }
                                }
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(splitsList) { index, split ->
                                        RemoteSplitRow(
                                            split = split,
                                            isActive = index == currentSplitIndex,
                                            isCompleted = index < currentSplitIndex,
                                            layoutPreferences = layoutPrefs
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (!layoutPrefs.isMinimalistMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!gameName.isNullOrBlank()) "$gameName — ${categoryName ?: ""}" else stringResource(R.string.remote_title),
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
                                fontSize = if (showSplitsInFullscreen) 70.sp else 110.sp,
                                lineHeight = if (showSplitsInFullscreen) 77.sp else 121.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = colors.timerText,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true
                        )

                        if (!layoutPrefs.isMinimalistMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when(remotePhase) {
                                    "Running" -> colors.success.copy(alpha = 0.2f)
                                    "Paused" -> colors.warning.copy(alpha = 0.2f)
                                    "Ended" -> colors.info.copy(alpha = 0.2f)
                                    else -> colors.textDisabled.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = getTranslatedPhase(remotePhase).uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when(remotePhase) {
                                        "Running" -> colors.success
                                        "Paused" -> colors.warning
                                        "Ended" -> colors.info
                                        else -> colors.textSecondary
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (!layoutPrefs.isMinimalistMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { }
                            ) {
                                TimerControls(
                                    timerState = timerState,
                                    isLastSplit = false,
                                    onStartSplit = { viewModel.sendCommand("startorsplit") },
                                    onPauseResume = { viewModel.sendCommand(if (remotePhase == "Paused") "resume" else "pause") },
                                    onUndo = { viewModel.sendCommand("unsplit") },
                                    onSkip = { viewModel.sendCommand("skipsplit") },
                                    onReset = { viewModel.sendCommand("reset") },
                                    showUndo = layoutPrefs.showUndoButton,
                                    showSkip = layoutPrefs.showSkipButton,
                                    showPause = layoutPrefs.showPauseButton
                                )
                            }
                        }
                    }
                }
            }

            val showFullscreenControls = !layoutPrefs.isMinimalistMode || remotePhase != "Running"

            if (showFullscreenControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if ((isSplitsListSupported && splitsList.isNotEmpty()) || !currentSplitName.isNullOrBlank()) {
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
                    }
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.remote_title), color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.textPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToLayoutEditor) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = colors.textPrimary)
                        }
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = colors.textPrimary)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = colors.error.copy(alpha = 0.1f))) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(errorMessage!!, color = colors.error, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) { Icon(Icons.Default.Close, null, tint = colors.error) }
                            }
                        }
                    }

                    if (isReconnecting) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.warning.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.warning.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.warning
                                )
                                Text(
                                    text = stringResource(R.string.remote_reconnecting, reconnectAttempts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 1. Connection Settings Block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { isSettingsExpanded = !isSettingsExpanded }, 
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.remote_conn_settings), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Icon(if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }
                            AnimatedVisibility(isSettingsExpanded) {
                                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = host, 
                                            onValueChange = { viewModel.updateHost(it) }, 
                                            label = { Text("IP") }, 
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = port, 
                                            onValueChange = { viewModel.updatePort(it) }, 
                                            label = { Text("Port") }, 
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { if (connectionState == ConnectionState.CONNECTED) viewModel.disconnect() else viewModel.connect() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (connectionState == ConnectionState.CONNECTED) colors.error else MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(if (connectionState == ConnectionState.CONNECTED) stringResource(R.string.remote_disconnect) else stringResource(R.string.remote_connect))
                                    }
                                }
                            }
                        }
                    }

                    if (connectionState == ConnectionState.CONNECTED && !gameName.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface.copy(alpha = 0.7f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = gameName!!,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                if (!categoryName.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = categoryName!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        if (isSplitsListSupported && splitsList.isNotEmpty()) {
                            val listState = rememberLazyListState()
                            val scrollTarget = (currentSplitIndex - 2).coerceAtLeast(0)
                            LaunchedEffect(scrollTarget) {
                                if (scrollTarget in splitsList.indices) {
                                    listState.animateScrollToItem(scrollTarget)
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface.copy(alpha = 0.7f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    itemsIndexed(splitsList) { index, split ->
                                        val isActive = index == currentSplitIndex
                                        val isCompleted = index < currentSplitIndex
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            color = if (isActive) colors.success.copy(alpha = 0.15f) else Color.Transparent,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = split.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isActive) colors.success else if (isCompleted) colors.textSecondary else colors.textPrimary
                                                    )
                                                }
                                                
                                                if (split.delta != null) {
                                                    Text(
                                                        text = split.delta,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (split.delta.startsWith("-")) colors.aheadGaining else colors.behindLosing,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }

                                                Text(
                                                    text = split.actualTime ?: "-",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                    color = if (isActive) colors.success else colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!currentSplitName.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface.copy(alpha = 0.7f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.remote_current_split),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentSplitName!!,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }
                                    if (currentSplitIndex >= 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.success.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "#${currentSplitIndex + 1}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.success
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Raw Timer display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { isFullscreen = true }) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = smoothRemoteTime, 
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp), 
                            fontWeight = FontWeight.Black, 
                            color = colors.timerText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when(remotePhase) {
                                "Running" -> colors.success.copy(alpha = 0.15f)
                                "Paused" -> colors.warning.copy(alpha = 0.15f)
                                else -> colors.textSecondary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = getTranslatedPhase(remotePhase).uppercase(), 
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.Bold,
                                color = when(remotePhase) {
                                    "Running" -> colors.success
                                    "Paused" -> colors.warning
                                    else -> colors.textSecondary
                                }
                            )
                        }
                    }

                    val currentElapsed = remember(smoothRemoteTime) { TimeSpan.fromTimeString(smoothRemoteTime) ?: TimeSpan.ZERO }
                    val timerState = remember(remotePhase, currentElapsed) {
                        when (remotePhase) {
                            "Running" -> TimerState.Running(0L, 0L, 0, emptyList(), ComparisonName.PERSONAL_BEST)
                            "Paused" -> TimerState.Paused(currentElapsed, 0, emptyList(), ComparisonName.PERSONAL_BEST)
                            "Ended" -> TimerState.Finished(currentElapsed, emptyList(), ComparisonName.PERSONAL_BEST)
                            else -> TimerState.Idle
                        }
                    }

                    TimerControls(
                        timerState = timerState,
                        isLastSplit = false,
                        onStartSplit = { viewModel.sendCommand("startorsplit") },
                        onPauseResume = { viewModel.sendCommand(if (remotePhase == "Paused") "resume" else "pause") },
                        onUndo = { viewModel.sendCommand("unsplit") },
                        onSkip = { viewModel.sendCommand("skipsplit") },
                        onReset = { viewModel.sendCommand("reset") }
                    )
                }
            }
        }
    }
}
