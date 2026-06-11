package com.elg.swiftsplit.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
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
    val timingMethod by viewModel.timingMethod.collectAsStateWithLifecycle()
    val comparison by viewModel.comparison.collectAsStateWithLifecycle()
    val saveQuickRuns by viewModel.saveQuickRuns.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val globalHotkeysEnabled by viewModel.globalHotkeysEnabled.collectAsStateWithLifecycle()
    val networkPreferences by viewModel.networkPreferences.collectAsStateWithLifecycle()
    val colors = SwiftSplitThemeColors.colors
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
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
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column {
                    Box {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_timing_method), color = colors.textPrimary) },
                            supportingContent = { Text(getTimingMethodDisplayName(timingMethod), color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { showTimingMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showTimingMenu,
                            onDismissRequest = { showTimingMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            TimingMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(getTimingMethodDisplayName(method)) },
                                    onClick = {
                                        viewModel.setTimingMethod(method)
                                        showTimingMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_active_comparison), color = colors.textPrimary) },
                            supportingContent = { Text(getComparisonDisplayName(comparison.name), color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { showComparisonMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showComparisonMenu,
                            onDismissRequest = { showComparisonMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val comps = listOf(
                                ComparisonName.PERSONAL_BEST,
                                ComparisonName.BEST_SEGMENTS,
                                ComparisonName.AVERAGE_SEGMENTS
                            )
                            comps.forEach { comp ->
                                DropdownMenuItem(
                                    text = { Text(getComparisonDisplayName(comp.name)) },
                                    onClick = {
                                        viewModel.setComparison(comp)
                                        showComparisonMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_save_quick_runs), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.settings_save_quick_runs_desc), color = colors.textSecondary) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = saveQuickRuns,
                                onCheckedChange = { viewModel.setSaveQuickRuns(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_global_hotkeys), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.settings_global_hotkeys_desc), color = colors.textSecondary) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = globalHotkeysEnabled,
                                onCheckedChange = { viewModel.setGlobalHotkeysEnabled(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            Text(
                stringResource(R.string.settings_network_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column {
                    Box {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_polling_delay), color = colors.textPrimary) },
                            supportingContent = { Text(stringResource(R.string.settings_polling_delay_desc), color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = stringResource(R.string.settings_network_ms_value, networkPreferences.pollingDelayMs),
                                    color = colors.textSecondary
                                )
                            },
                            modifier = Modifier.clickable { showPollingDelayMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showPollingDelayMenu,
                            onDismissRequest = { showPollingDelayMenu = false },
                            shape = RoundedCornerShape(12.dp)
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

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_network_timeout), color = colors.textPrimary) },
                            supportingContent = { Text(stringResource(R.string.settings_network_timeout_desc), color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = stringResource(R.string.settings_network_ms_value, networkPreferences.networkTimeoutMs),
                                    color = colors.textSecondary
                                )
                            },
                            modifier = Modifier.clickable { showNetworkTimeoutMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showNetworkTimeoutMenu,
                            onDismissRequest = { showNetworkTimeoutMenu = false },
                            shape = RoundedCornerShape(12.dp)
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
                }
            }

            Text(
                stringResource(R.string.settings_appearance_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
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
                        headlineContent = { Text(stringResource(R.string.settings_customization), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.settings_customization_desc), color = colors.textSecondary) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onNavigateToLayoutEditor() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box {
                        val themeLabel = when(themeMode) {
                            "light" -> stringResource(R.string.settings_theme_light)
                            "dark" -> stringResource(R.string.settings_theme_dark)
                            else -> stringResource(R.string.settings_theme_system)
                        }
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_theme), color = colors.textPrimary) },
                            supportingContent = { Text(themeLabel, color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { showThemeMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false },
                            shape = RoundedCornerShape(12.dp)
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

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Box {
                        val langLabel = when(language) {
                            "en" -> stringResource(R.string.settings_language_en)
                            "fr" -> stringResource(R.string.settings_language_fr)
                            "es" -> stringResource(R.string.settings_language_es)
                            "de" -> stringResource(R.string.settings_language_de)
                            "it" -> stringResource(R.string.settings_language_it)
                            "pt" -> stringResource(R.string.settings_language_pt)
                            "ja" -> stringResource(R.string.settings_language_ja)
                            "zh" -> stringResource(R.string.settings_language_zh)
                            "ru" -> stringResource(R.string.settings_language_ru)
                            "ko" -> stringResource(R.string.settings_language_ko)
                            "pl" -> stringResource(R.string.settings_language_pl)
                            else -> stringResource(R.string.settings_language_auto)
                        }
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_language), color = colors.textPrimary) },
                            supportingContent = { Text(langLabel, color = colors.textSecondary) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { showLanguageMenu = true },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            shape = RoundedCornerShape(12.dp)
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_es)) },
                                onClick = { viewModel.setLanguage("es"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_de)) },
                                onClick = { viewModel.setLanguage("de"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_it)) },
                                onClick = { viewModel.setLanguage("it"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_pt)) },
                                onClick = { viewModel.setLanguage("pt"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_ja)) },
                                onClick = { viewModel.setLanguage("ja"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_zh)) },
                                onClick = { viewModel.setLanguage("zh"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_ru)) },
                                onClick = { viewModel.setLanguage("ru"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_ko)) },
                                onClick = { viewModel.setLanguage("ko"); showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_pl)) },
                                onClick = { viewModel.setLanguage("pl"); showLanguageMenu = false }
                            )
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.settings_about_header),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
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
                        headlineContent = { Text(stringResource(R.string.settings_rate_app), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.settings_rate_app_desc), color = colors.textSecondary) },
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
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = colors.deepBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_about_title), color = colors.textPrimary) },
                        supportingContent = { Text(stringResource(R.string.settings_about_desc), color = colors.textSecondary) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onNavigateToAbout() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun getTimingMethodDisplayName(method: TimingMethod): String {
    return when (method) {
        TimingMethod.REAL_TIME -> stringResource(R.string.timing_method_real_time)
        TimingMethod.GAME_TIME -> stringResource(R.string.timing_method_game_time)
    }
}

@Composable
fun getComparisonDisplayName(name: String): String {
    return when (name) {
        "Personal Best" -> stringResource(R.string.comparison_pb)
        "Best Segments" -> stringResource(R.string.comparison_best_segments)
        "Average Segments" -> stringResource(R.string.comparison_average_segments)
        "Worst Segments" -> stringResource(R.string.comparison_worst_segments)
        "Median Segments" -> stringResource(R.string.comparison_median_segments)
        "Latest Run" -> stringResource(R.string.comparison_latest_run)
        else -> name
    }
}
