package com.elg.swiftsplit.ui.screen.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
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
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.ui.screen.timer.components.TimerControls
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.view.WindowManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import com.elg.swiftsplit.R
import com.elg.swiftsplit.domain.model.TimerState
import com.elg.swiftsplit.domain.model.ComparisonName
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
    val layoutPrefs by viewModel.timerLayoutPreferences.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    val smoothRemoteTime = rememberAnimatedRemoteTime(remoteTime, remotePhase, layoutPrefs.timeFormat)
    val colors = SwiftSplitThemeColors.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }

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

    // Fullscreen Orientation Handling
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
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.PORTRAIT -> true
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.LANDSCAPE -> false
            com.elg.swiftsplit.domain.model.FullscreenOrientationPreset.AUTO ->
                configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
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
                            top = if (layoutPrefs.isMinimalistMode) 16.dp else 48.dp,
                            bottom = 24.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!layoutPrefs.isMinimalistMode) {
                        Text(
                            text = stringResource(R.string.remote_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = smoothRemoteTime,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
                            fontWeight = FontWeight.Black,
                            color = colors.timerText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when(remotePhase) {
                                "Running" -> colors.success.copy(alpha = 0.2f)
                                "Paused" -> colors.warning.copy(alpha = 0.2f)
                                else -> colors.textSecondary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = getTranslatedPhase(remotePhase).uppercase(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = when(remotePhase) {
                                    "Running" -> colors.success
                                    "Paused" -> colors.warning
                                    else -> colors.textSecondary
                                }
                            )
                        }
                    }

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
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 32.dp,
                            bottom = 24.dp,
                            start = 32.dp,
                            end = 32.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = smoothRemoteTime,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                            fontWeight = FontWeight.Black,
                            color = colors.timerText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when(remotePhase) {
                                "Running" -> colors.success.copy(alpha = 0.2f)
                                "Paused" -> colors.warning.copy(alpha = 0.2f)
                                else -> colors.textSecondary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = getTranslatedPhase(remotePhase).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when(remotePhase) {
                                    "Running" -> colors.success
                                    "Paused" -> colors.warning
                                    else -> colors.textSecondary
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
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

            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
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
                                        Text(if (connectionState == ConnectionState.CONNECTED) "Disconnect" else "Connect")
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
                    // Raw Timer display (No surrounding block Card / border)
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
