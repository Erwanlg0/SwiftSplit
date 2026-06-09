package com.elg.swiftsplit.ui.screen.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elg.swiftsplit.ui.theme.SpeedrunThemeColors
import com.elg.swiftsplit.R

import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEditorScreen(
    runId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplitEditorViewModel = hiltViewModel()
) {
    val run by viewModel.run.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val colors = SpeedrunThemeColors.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title), color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onNavigateBack) }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.deepBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addSegment() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.editor_add_split))
            }
        },
        containerColor = colors.deepBackground
    ) { innerPadding ->
        val currentRun = run
        if (currentRun == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = currentRun.gameInfo.gameName,
                    onValueChange = { viewModel.updateGameName(it) },
                    label = { Text(stringResource(R.string.editor_game_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentRun.gameInfo.categoryName,
                    onValueChange = { viewModel.updateCategoryName(it) },
                    label = { Text(stringResource(R.string.editor_category_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.editor_splits_count, segments.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(segments) { index, segment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.elevatedSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = segment.name,
                                    onValueChange = { viewModel.updateSegmentName(index, it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.editor_split_name_placeholder)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { viewModel.moveSegment(index, index - 1) },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = stringResource(R.string.editor_move_up),
                                        tint = if (index > 0) colors.textPrimary else colors.textDisabled
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.moveSegment(index, index + 1) },
                                    enabled = index < segments.size - 1
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = stringResource(R.string.editor_move_down),
                                        tint = if (index < segments.size - 1) colors.textPrimary else colors.textDisabled
                                    )
                                }

                                IconButton(onClick = { viewModel.removeSegment(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = colors.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
