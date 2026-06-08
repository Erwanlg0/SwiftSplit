package com.elg.speedruncompanion.ui.screen.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.speedruncompanion.application.port.output.ConnectionState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val remoteTime by viewModel.remoteTime.collectAsState()
    val remotePhase by viewModel.remotePhase.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val colors = SpeedrunThemeColors.colors

    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }

    // Automatically collapse settings when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            isSettingsExpanded = false
        } else if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
            isSettingsExpanded = true
        }
    }

    // Manage screen orientation based on fullscreen state
    androidx.compose.runtime.DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        if (isFullscreen) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            isFullscreen = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "LIVESPLIT REMOTE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTertiary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = remoteTime,
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
                        text = remotePhase.uppercase(),
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

            // Exit button top-right
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
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.deepBackground
                    )
                )
            },
            containerColor = colors.deepBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Error Banner
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
                        // Clickable Header to Expand/Collapse settings
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

                        // Collapsible settings fields
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

                if (connectionState == ConnectionState.CONNECTED) {
                    // Live Timer Display
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
                                text = remoteTime,
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
                            Spacer(modifier = Modifier.height(4.dp))
                            // Show current phase badge
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
                                    text = remotePhase,
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

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Button(
                                onClick = { viewModel.sendCommand("startorsplit") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_btn_split))
                            }
                        }

                        item {
                            val isPaused = remotePhase.equals("Paused", ignoreCase = true)
                            Button(
                                onClick = { viewModel.sendCommand(if (isPaused) "resume" else "pause") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.info)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(
                                        if (isPaused) R.string.remote_btn_resume else R.string.remote_btn_pause
                                    )
                                )
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.sendCommand("unsplit") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.warning)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_btn_undo))
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.sendCommand("skipsplit") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textSecondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_btn_skip))
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.sendCommand("reset") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_btn_reset))
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.sendCommand("ping") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.elevatedSurface)
                            ) {
                                Icon(Icons.Default.CastConnected, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_btn_ping))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                                color = colors.textSecondary
                            )
                            Text(
                                stringResource(R.string.remote_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
