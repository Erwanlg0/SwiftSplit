package com.elg.swiftsplit.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.ui.theme.SpeedrunThemeColors
import com.elg.swiftsplit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLayoutEditor: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val timingMethod by viewModel.timingMethod.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val saveQuickRuns by viewModel.saveQuickRuns.collectAsState()
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val globalHotkeysEnabled by viewModel.globalHotkeysEnabled.collectAsState()
    val networkPreferences by viewModel.networkPreferences.collectAsState()
    val colors = SpeedrunThemeColors.colors
    val context = LocalContext.current

    var showTimingMenu by remember { mutableStateOf(false) }
    var showComparisonMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPollingDelayMenu by remember { mutableStateOf(false) }
    var showNetworkTimeoutMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = colors.textPrimary) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Text(
                stringResource(R.string.settings_timer_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_timing_method)) },
                    supportingContent = { Text(timingMethod.name) },
                    modifier = Modifier.clickable { showTimingMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showTimingMenu,
                    onDismissRequest = { showTimingMenu = false }
                ) {
                    TimingMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name) },
                            onClick = {
                                viewModel.setTimingMethod(method)
                                showTimingMenu = false
                            }
                        )
                    }
                }
            }

            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_active_comparison)) },
                    supportingContent = { Text(comparison.name) },
                    modifier = Modifier.clickable { showComparisonMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showComparisonMenu,
                    onDismissRequest = { showComparisonMenu = false }
                ) {
                    val comps = listOf(
                        ComparisonName.PERSONAL_BEST,
                        ComparisonName.BEST_SEGMENTS,
                        ComparisonName.AVERAGE_SEGMENTS
                    )
                    comps.forEach { comp ->
                        DropdownMenuItem(
                            text = { Text(comp.name) },
                            onClick = {
                                viewModel.setComparison(comp)
                                showComparisonMenu = false
                            }
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_save_quick_runs)) },
                supportingContent = { Text(stringResource(R.string.settings_save_quick_runs_desc)) },
                trailingContent = {
                    Switch(
                        checked = saveQuickRuns,
                        onCheckedChange = { viewModel.setSaveQuickRuns(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_global_hotkeys)) },
                supportingContent = { Text(stringResource(R.string.settings_global_hotkeys_desc)) },
                trailingContent = {
                    Switch(
                        checked = globalHotkeysEnabled,
                        onCheckedChange = { viewModel.setGlobalHotkeysEnabled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            
            Text(
                stringResource(R.string.settings_network_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_polling_delay)) },
                    supportingContent = { Text(stringResource(R.string.settings_polling_delay_desc)) },
                    trailingContent = {
                        Text(
                            text = stringResource(R.string.settings_network_ms_value, networkPreferences.pollingDelayMs),
                            color = colors.textSecondary
                        )
                    },
                    modifier = Modifier.clickable { showPollingDelayMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showPollingDelayMenu,
                    onDismissRequest = { showPollingDelayMenu = false }
                ) {
                    NetworkPreferences.POLLING_DELAY_OPTIONS.forEach { delayMs ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_network_ms_value, delayMs)) },
                            onClick = {
                                viewModel.setPollingDelayMs(delayMs)
                                showPollingDelayMenu = false
                            }
                        )
                    }
                }
            }

            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_network_timeout)) },
                    supportingContent = { Text(stringResource(R.string.settings_network_timeout_desc)) },
                    trailingContent = {
                        Text(
                            text = stringResource(R.string.settings_network_ms_value, networkPreferences.networkTimeoutMs),
                            color = colors.textSecondary
                        )
                    },
                    modifier = Modifier.clickable { showNetworkTimeoutMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showNetworkTimeoutMenu,
                    onDismissRequest = { showNetworkTimeoutMenu = false }
                ) {
                    NetworkPreferences.NETWORK_TIMEOUT_OPTIONS.forEach { timeoutMs ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_network_ms_value, timeoutMs)) },
                            onClick = {
                                viewModel.setNetworkTimeoutMs(timeoutMs)
                                showNetworkTimeoutMenu = false
                            }
                        )
                    }
                }
            }

            
            Text(
                stringResource(R.string.settings_appearance_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_customization)) },
                supportingContent = { Text(stringResource(R.string.settings_customization_desc)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onNavigateToLayoutEditor() },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            Box {
                val themeLabel = when(themeMode) {
                    "light" -> stringResource(R.string.settings_theme_light)
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme)) },
                    supportingContent = { Text(themeLabel) },
                    leadingContent = {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { showThemeMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_theme_system)) },
                        onClick = { viewModel.setThemeMode("system"); showThemeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_theme_light)) },
                        onClick = { viewModel.setThemeMode("light"); showThemeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_theme_dark)) },
                        onClick = { viewModel.setThemeMode("dark"); showThemeMenu = false }
                    )
                }
            }

            Box {
                val langLabel = when(language) {
                    "en" -> stringResource(R.string.settings_language_en)
                    "fr" -> stringResource(R.string.settings_language_fr)
                    else -> stringResource(R.string.settings_language_auto)
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text(langLabel) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { showLanguageMenu = true },
                    colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
                )
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_language_auto)) },
                        onClick = { viewModel.setLanguage("auto"); showLanguageMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_language_en)) },
                        onClick = { viewModel.setLanguage("en"); showLanguageMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_language_fr)) },
                        onClick = { viewModel.setLanguage("fr"); showLanguageMenu = false }
                    )
                }
            }

            
            Text(
                stringResource(R.string.settings_about_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rate_app)) },
                supportingContent = { Text(stringResource(R.string.settings_rate_app_desc)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable {
                    val packageName = context.packageName
                    val marketUri = android.net.Uri.parse("market://details?id=$packageName")
                    val webUri = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    try {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, marketUri)
                        )
                    } catch (e: android.content.ActivityNotFoundException) {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, webUri)
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                modifier = Modifier.clickable { onNavigateToAbout() },
                colors = ListItemDefaults.colors(containerColor = colors.elevatedSurface)
            )
        }
    }
}
