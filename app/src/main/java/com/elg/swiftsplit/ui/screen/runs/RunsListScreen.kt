package com.elg.swiftsplit.ui.screen.runs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elg.swiftsplit.ui.screen.runs.components.RunCard
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors
import android.widget.Toast
import com.elg.swiftsplit.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunsListScreen(
    onRunClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onRemoteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val swiftSplitColors = SwiftSplitThemeColors.colors

    var showNewRunDialog by remember { mutableStateOf(false) }
    var showCustomRunForm by remember { mutableStateOf(false) }
    var customGameName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var customPlatform by remember { mutableStateOf("") }

    var showUrlImportDialog by remember { mutableStateOf(false) }
    var urlToImport by remember { mutableStateOf("") }
    var isImportingFromUrl by remember { mutableStateOf(false) }

    var showSpeedrunDialog by remember { mutableStateOf(false) }
    var speedrunQuery by remember { mutableStateOf("") }
    val speedrunGames by viewModel.speedrunGames.collectAsStateWithLifecycle()
    val speedrunCategories by viewModel.speedrunCategories.collectAsStateWithLifecycle()
    val speedrunRuns by viewModel.speedrunRuns.collectAsStateWithLifecycle()
    val isSpeedrunLoading by viewModel.isSpeedrunLoading.collectAsStateWithLifecycle()

    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var selectedGameName by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var onlyRunsWithSplits by remember { mutableStateOf(false) }

    
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    viewModel.importRun(input.readBytes())
                }
            } catch (e: Exception) {
                
            }
        }
    }

    
    if (showNewRunDialog) {
        AlertDialog(
            onDismissRequest = { showNewRunDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.new_run_dialog_title),
                    color = swiftSplitColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNewRunDialog = false
                                viewModel.createQuickRun { runId ->
                                    onRunClick(runId)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.new_run_dialog_quick),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_run_dialog_quick_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = swiftSplitColors.textSecondary
                            )
                        }
                    }

                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNewRunDialog = false
                                showCustomRunForm = true
                            },
                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.new_run_dialog_custom),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_run_dialog_custom_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = swiftSplitColors.textSecondary
                            )
                        }
                    }

                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNewRunDialog = false
                                fileLauncher.launch("*/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.new_run_dialog_import),
                                fontWeight = FontWeight.Bold,
                                color = swiftSplitColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_run_dialog_import_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = swiftSplitColors.textSecondary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNewRunDialog = false
                                showUrlImportDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.new_run_dialog_import_url),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_run_dialog_import_url_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = swiftSplitColors.textSecondary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNewRunDialog = false
                                showSpeedrunDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.new_run_dialog_speedrun),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_run_dialog_speedrun_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = swiftSplitColors.textSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewRunDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = swiftSplitColors.textSecondary
                    )
                }
            },
            containerColor = swiftSplitColors.cardBackground
        )
    }

    // URL Import Dialog - at top level so it persists after parent dialog closes
    if (showUrlImportDialog) {
        AlertDialog(
            onDismissRequest = { showUrlImportDialog = false; urlToImport = "" },
            title = { Text(stringResource(R.string.runs_list_import_url_title), color = swiftSplitColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = urlToImport,
                        onValueChange = { urlToImport = it },
                        placeholder = { Text(stringResource(R.string.runs_list_import_url_placeholder)) },
                        label = { Text(stringResource(R.string.runs_list_import_url_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isImportingFromUrl) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImportingFromUrl = true
                        viewModel.importRunFromUrl(
                            url = urlToImport,
                            onSuccess = {
                                isImportingFromUrl = false
                                showUrlImportDialog = false
                                urlToImport = ""
                                Toast.makeText(context, context.getString(R.string.runs_list_import_success), Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                isImportingFromUrl = false
                                Toast.makeText(context, context.getString(R.string.runs_list_import_error, err.localizedMessage), Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = urlToImport.isNotBlank() && !isImportingFromUrl
                ) {
                    Text(stringResource(R.string.runs_list_import_url_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlImportDialog = false; urlToImport = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = swiftSplitColors.cardBackground
        )
    }


    if (showSpeedrunDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSpeedrunDialog = false
                speedrunQuery = ""
                selectedGameId = null
                selectedCategoryId = null
                viewModel.clearSpeedrunSearch()
            },
            title = {
                Text(
                    text = stringResource(R.string.runs_list_speedrun_dialog_title),
                    color = swiftSplitColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    if (selectedGameId == null) {
                        OutlinedTextField(
                            value = speedrunQuery,
                            onValueChange = { speedrunQuery = it },
                            placeholder = { Text(stringResource(R.string.runs_list_speedrun_game_placeholder)) },
                            label = { Text(stringResource(R.string.runs_list_speedrun_game_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.searchSpeedrunGames(speedrunQuery) }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isSpeedrunLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(speedrunGames, key = { it.id }) { game ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedGameId = game.id
                                                selectedGameName = game.names.international
                                                viewModel.selectSpeedrunGame(game.id)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                                    ) {
                                        Text(
                                            text = game.names.international,
                                            modifier = Modifier.padding(12.dp),
                                            color = swiftSplitColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedCategoryId == null) {
                        Text(stringResource(R.string.runs_list_speedrun_selected_game, selectedGameName ?: ""), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = swiftSplitColors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSpeedrunLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(speedrunCategories, key = { it.id }) { cat ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategoryId = cat.id
                                                selectedCategoryName = cat.name
                                                viewModel.selectSpeedrunCategory(selectedGameId!!, cat.id)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                                    ) {
                                        Text(
                                            text = cat.name,
                                            modifier = Modifier.padding(12.dp),
                                            color = swiftSplitColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.runs_list_speedrun_selected_game, selectedGameName ?: ""), style = MaterialTheme.typography.bodySmall, color = swiftSplitColors.textSecondary)
                        Text(stringResource(R.string.runs_list_speedrun_selected_category, selectedCategoryName ?: ""), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = swiftSplitColors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isSpeedrunLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            val filteredRuns = if (onlyRunsWithSplits) {
                                speedrunRuns.filter { it.run.splits != null }
                            } else {
                                speedrunRuns
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onlyRunsWithSplits = !onlyRunsWithSplits }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = onlyRunsWithSplits,
                                    onCheckedChange = { onlyRunsWithSplits = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = swiftSplitColors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.runs_list_speedrun_only_splits),
                                    color = swiftSplitColors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (filteredRuns.isEmpty()) {
                                Text(stringResource(R.string.runs_list_speedrun_no_runs), style = MaterialTheme.typography.bodyMedium, color = swiftSplitColors.textSecondary)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredRuns, key = { it.run.id }) { placement ->
                                        val hasSplits = placement.run.splits != null
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!hasSplits) {
                                                        Toast.makeText(context, context.getString(R.string.runs_list_speedrun_no_splits_toast), Toast.LENGTH_LONG).show()
                                                    } else {
                                                        showSpeedrunDialog = false
                                                        viewModel.importSpeedrunRun(
                                                            runId = placement.run.id,
                                                            onSuccess = {
                                                                Toast.makeText(context, context.getString(R.string.runs_list_speedrun_import_success), Toast.LENGTH_SHORT).show()
                                                                viewModel.clearSpeedrunSearch()
                                                            },
                                                            onFailure = { err ->
                                                                Toast.makeText(context, context.getString(R.string.runs_list_import_error, err.localizedMessage), Toast.LENGTH_LONG).show()
                                                                viewModel.clearSpeedrunSearch()
                                                            }
                                                        )
                                                    }
                                                },
                                            colors = CardDefaults.cardColors(containerColor = swiftSplitColors.elevatedSurface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val timeSec = placement.run.times.primary_t
                                                    val formattedTime = if (timeSec >= 3600) {
                                                        String.format("%d:%02d:%02d", (timeSec / 3600).toInt(), ((timeSec % 3600) / 60).toInt(), (timeSec % 60).toInt())
                                                    } else {
                                                        String.format("%02d:%02d", (timeSec / 60).toInt(), (timeSec % 60).toInt())
                                                    }
                                                    Text(
                                                        text = stringResource(R.string.runs_list_speedrun_place_format, placement.place, formattedTime),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (hasSplits) swiftSplitColors.textPrimary else swiftSplitColors.textDisabled
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.runs_list_speedrun_run_id_format, placement.run.id),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = swiftSplitColors.textSecondary
                                                    )
                                                }
                                                if (hasSplits) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        shape = MaterialTheme.shapes.small
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.runs_list_speedrun_badge_importable),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        color = swiftSplitColors.textDisabled.copy(alpha = 0.1f),
                                                        shape = MaterialTheme.shapes.small
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.runs_list_speedrun_badge_no_splits),
                                                            color = swiftSplitColors.textDisabled,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        if (selectedCategoryId != null) {
                            selectedCategoryId = null
                            viewModel.selectSpeedrunGame(selectedGameId!!)
                        } else if (selectedGameId != null) {
                            selectedGameId = null
                            viewModel.clearSpeedrunSearch()
                        } else {
                            showSpeedrunDialog = false
                            viewModel.clearSpeedrunSearch()
                        }
                    }
                ) {
                    Text(if (selectedGameId != null) stringResource(R.string.back) else stringResource(R.string.close))
                }
            },
            containerColor = swiftSplitColors.cardBackground
        )
    }

    
    if (showCustomRunForm) {
        AlertDialog(
            onDismissRequest = { 
                showCustomRunForm = false
                customGameName = ""
                customCategory = ""
                customPlatform = ""
            },
            title = {
                Text(
                    text = stringResource(R.string.new_run_dialog_custom),
                    color = swiftSplitColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customGameName,
                        onValueChange = { customGameName = it },
                        label = { Text(stringResource(R.string.new_run_dialog_field_game)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = swiftSplitColors.textPrimary,
                            unfocusedTextColor = swiftSplitColors.textSecondary
                        )
                    )

                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text(stringResource(R.string.new_run_dialog_field_category)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = swiftSplitColors.textPrimary,
                            unfocusedTextColor = swiftSplitColors.textSecondary
                        )
                    )

                    OutlinedTextField(
                        value = customPlatform,
                        onValueChange = { customPlatform = it },
                        label = { Text(stringResource(R.string.new_run_dialog_field_platform)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = swiftSplitColors.textPrimary,
                            unfocusedTextColor = swiftSplitColors.textSecondary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val game = customGameName
                        val cat = customCategory
                        val plat = customPlatform
                        showCustomRunForm = false
                        customGameName = ""
                        customCategory = ""
                        customPlatform = ""
                        viewModel.createNewRun(game, cat, plat) { runId ->
                            onEditClick(runId)
                        }
                    }
                ) {
                    Text(stringResource(R.string.new_run_dialog_create))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCustomRunForm = false
                        customGameName = ""
                        customCategory = ""
                        customPlatform = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = swiftSplitColors.cardBackground
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.runs_list_title),
                        fontWeight = FontWeight.Bold,
                        color = swiftSplitColors.textPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onRemoteClick) {
                        Icon(
                            Icons.Default.CastConnected,
                            contentDescription = stringResource(R.string.runs_list_pc_remote),
                            tint = swiftSplitColors.textPrimary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.runs_list_settings),
                            tint = swiftSplitColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = swiftSplitColors.deepBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewRunDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_run_dialog_title))
            }
        },
        containerColor = swiftSplitColors.deepBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is RunsListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RunsListUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.runs_list_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = swiftSplitColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.runs_list_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = swiftSplitColors.textTertiary
                        )
                    }
                }
                is RunsListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.runs, key = { it.id.value }) { run ->
                            RunCard(
                                run = run,
                                onClick = { onRunClick(run.id.value) },
                                onEditClick = { onEditClick(run.id.value) },
                                onDeleteClick = { viewModel.deleteRun(run.id) }
                            )
                        }
                    }
                }
                is RunsListUiState.Error -> {
                    Text(
                        text = stringResource(R.string.runs_list_import_error, state.message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = swiftSplitColors.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
