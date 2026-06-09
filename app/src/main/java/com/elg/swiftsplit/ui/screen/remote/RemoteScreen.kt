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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elg.swiftsplit.domain.model.TimeFormatOptions
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.ui.theme.SpeedrunThemeColors
import com.elg.swiftsplit.R

import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.withFrameMillis
import com.elg.swiftsplit.domain.model.TimeSpan

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
            var lastParsedTimeSpan = TimeSpan.fromTimeString(latestRawTime.value) ?: TimeSpan.ZERO
            var lastRawValue = latestRawTime.value
            var baseFrameTime = -1L
            var timeSpanAtBase = lastParsedTimeSpan

            while (true) {
                withFrameMillis { frameTime ->
                    if (latestRawTime.value != lastRawValue) {
                        lastRawValue = latestRawTime.value
                        lastParsedTimeSpan = TimeSpan.fromTimeString(lastRawValue) ?: TimeSpan.ZERO
                        baseFrameTime = frameTime
                        timeSpanAtBase = lastParsedTimeSpan
                    }

                    if (baseFrameTime == -1L) {
                        baseFrameTime = frameTime
                        timeSpanAtBase = lastParsedTimeSpan
                    }

                    val elapsedMs = frameTime - baseFrameTime
                    val currentElapsed = if (elapsedMs > 0) elapsedMs else 0L
                    val interpolatedTime = TimeSpan(timeSpanAtBase.totalMilliseconds + currentElapsed)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLayoutEditor: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val remoteTime by viewModel.remoteTime.collectAsState()
    val remotePhase by viewModel.remotePhase.collectAsState()
    val remoteSplitName by viewModel.remoteSplitName.collectAsState()
    val remoteSplitIndex by viewModel.remoteSplitIndex.collectAsState()
    val remoteDelta by viewModel.remoteDelta.collectAsState()
    val layoutPrefs by viewModel.timerLayoutPreferences.collectAsState()
    val smoothRemoteTime = rememberAnimatedRemoteTime(remoteTime, remotePhase, layoutPrefs.timeFormat)
    val errorMessage by viewModel.errorMessage.collectAsState()
    val colors = SpeedrunThemeColors.colors
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
            android.widget.Toast.makeText(context, "Response: $lastResponse", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    
    androidx.compose.runtime.DisposableEffect(isFullscreen) {
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

    
    if (showTutorial) {
        var currentStep by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            title = {
                Text(
                    text = "Tutoriel Connexion PC (Étape $currentStep / 6)",
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
                            text = "1. Téléchargez LiveSplit depuis le site officiel :",
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
                            text = "(cliquez sur le lien pour l'ouvrir dans votre navigateur)",
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
                            text = "6. Enfin, cliquez sur le bouton 'Se connecter' (ou 'Connect') pour établir la connexion avec LiveSplit sur votre PC !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val (imageRes, stepDesc) = when (currentStep) {
                            2 -> Pair(
                                R.drawable.tuto_2,
                                "2. Faites un clic droit sur LiveSplit puis cliquez sur le bouton 'Settings'."
                            )
                            3 -> Pair(
                                R.drawable.tuto_3,
                                "3. Dans les paramètres, réglez 'Startup Behavior' à 'Start TCP Server' pour que la communication puisse se faire dès le démarrage."
                            )
                            4 -> Pair(
                                R.drawable.tuto_4,
                                "4. Si le serveur n'est pas lancé, faites un clic droit -> Control -> Start TCP Server."
                            )
                            else -> Pair(
                                R.drawable.tuto_5,
                                "5. Renseignez l'adresse IP locale de votre PC et modifiez le port si vous l'avez changé."
                            )
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
                    Text(if (currentStep < 6) "Suivant" else "Terminer")
                }
            },
            dismissButton = {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Précédent", color = colors.textSecondary)
                    }
                } else {
                    TextButton(onClick = { showTutorial = false }) {
                        Text("Fermer", color = colors.textSecondary)
                    }
                }
            },
            containerColor = colors.elevatedSurface
        )
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    val isPaused = remotePhase.equals("Paused", ignoreCase = true)
                    viewModel.sendCommand(if (isPaused) "resume" else "pause")
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = smoothRemoteTime,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp),
                    fontWeight = FontWeight.Black,
                    color = when (remotePhase) {
                        "Running" -> colors.timerText
                        "Paused" -> colors.warning
                        "Ended" -> colors.success
                        else -> colors.textTertiary
                    },
                    textAlign = TextAlign.Center
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
            }

            
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
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
                                    text = "Format du Chronomètre",
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
                                        headlineContent = { Text("Sélectionner le format") },
                                        supportingContent = {
                                            Text(
                                                text = "${getPatternDisplayName(currentPattern)}\nEx (1s) : ${sampleShort.formatted(layoutPrefs.timeFormat)}\nEx (1h) : ${sampleLong.formatted(layoutPrefs.timeFormat)}",
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
                                                            text = "Ex (1s): ${sampleShort.formatted(TimeFormatOptions(pattern = pat))} | (1h): ${sampleLong.formatted(TimeFormatOptions(pattern = pat))}",
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
                        }                  }
                    }
                }

                if (connectionState == ConnectionState.CONNECTED) {
                    
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
                            Button(
                                onClick = { viewModel.sendCommand(if (isPaused) "resume" else "pause") },
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
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
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
                } else {
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
