package com.elg.swiftsplit.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elg.swiftsplit.R
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.ui.theme.SwiftSplitThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunStatsScreen(
    runId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SwiftSplitThemeColors.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Statistiques & Historique", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        val run = uiState.run
        if (run == null) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game and Category Header
                Text(
                    text = run.gameInfo.gameName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = run.gameInfo.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Key metrics Grid (Rows and Columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Tentatives",
                        value = "${uiState.totalAttempts}",
                        description = "Resets : ${uiState.resetCount}",
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    StatCard(
                        title = "Complétés",
                        value = "${uiState.completedAttemptsCount}",
                        description = "Taux : %.1f%%".format(uiState.completionRate),
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pbStr = run.personalBest?.realTime?.formatted() ?: run.personalBest?.gameTime?.formatted() ?: "--:--"
                    StatCard(
                        title = "Record Perso (PB)",
                        value = pbStr,
                        description = "Meilleur temps final",
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    val sobStr = if (uiState.sumOfBest.totalMilliseconds > 0) uiState.sumOfBest.formatted() else "--:--"
                    StatCard(
                        title = "Sum of Best (SOB)",
                        value = sobStr,
                        description = "Somme des golds",
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }

                // Chart 1: PB Progression (Line Chart)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Progression du Record Personnel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.pbProgression.size < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Complétez au moins 2 runs pour tracer le graphique de progression.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textTertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            PbProgressionChart(
                                progression = uiState.pbProgression,
                                colors = colors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }

                // Chart 2: Segments Analytics (Gold vs Avg)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Analyse de régularité par segment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.segmentStats.isEmpty()) {
                            Text("Aucun segment défini.", color = colors.textTertiary)
                        } else {
                            uiState.segmentStats.forEach { stat ->
                                SegmentCompareRow(
                                    stat = stat,
                                    maxTimeMs = uiState.segmentStats.mapNotNull { it.averageTime?.totalMilliseconds ?: it.bestTime?.totalMilliseconds }.maxOrNull() ?: 1000L,
                                    colors = colors
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
    colors: com.elg.swiftsplit.ui.theme.SwiftSplitColorScheme
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        }
    }
}

@Composable
fun PbProgressionChart(
    progression: List<Pair<Int, TimeSpan>>,
    colors: com.elg.swiftsplit.ui.theme.SwiftSplitColorScheme,
    modifier: Modifier = Modifier
) {
    val points = progression.map { it.second.totalMilliseconds.toFloat() }
    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1000f
    val deltaVal = if (maxVal - minVal == 0f) 1f else maxVal - minVal

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        val graphWidth = width - 2 * padding
        val graphHeight = height - 2 * padding

        // Draw axes/grid
        drawLine(
            color = colors.textDisabled.copy(alpha = 0.2f),
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = colors.textDisabled.copy(alpha = 0.2f),
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )

        // Plot points and lines
        val path = Path()
        val pointsCoords = mutableListOf<Offset>()

        progression.forEachIndexed { index, pair ->
            val x = padding + (index.toFloat() / (progression.size - 1)) * graphWidth
            val yValue = pair.second.totalMilliseconds.toFloat()
            val y = padding + (1f - (yValue - minVal) / deltaVal) * graphHeight
            val coord = Offset(x, y)
            pointsCoords.add(coord)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw path with gradient stroke
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(colors.aheadGaining, colors.timerText)),
            style = Stroke(width = 6f)
        )

        // Draw drop gradient shadow under the line chart
        val fillPath = Path().apply {
            addPath(path)
            lineTo(pointsCoords.last().x, height - padding)
            lineTo(pointsCoords.first().x, height - padding)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(colors.timerText.copy(alpha = 0.15f), Color.Transparent),
                startY = pointsCoords.map { it.y }.minOrNull() ?: 0f,
                endY = height - padding
            )
        )

        // Draw points nodes
        pointsCoords.forEach { coord ->
            drawCircle(
                color = colors.timerText,
                radius = 8f,
                center = coord
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = coord
            )
        }
    }
}

@Composable
fun SegmentCompareRow(
    stat: SegmentStat,
    maxTimeMs: Long,
    colors: com.elg.swiftsplit.ui.theme.SwiftSplitColorScheme
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stat.name, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            val avgStr = stat.averageTime?.formatted() ?: "--:--"
            val bestStr = stat.bestTime?.formatted() ?: "--:--"
            Text(
                text = "Moy : $avgStr | Or : $bestStr",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Average Time Bar
        if (stat.averageTime != null) {
            val avgRatio = (stat.averageTime.totalMilliseconds.toFloat() / maxTimeMs.toFloat()).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(avgRatio)
                    .height(8.dp)
                    .background(colors.textDisabled.copy(alpha = 0.4f), MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Gold Time Bar
        if (stat.bestTime != null) {
            val bestRatio = (stat.bestTime.totalMilliseconds.toFloat() / maxTimeMs.toFloat()).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(bestRatio)
                    .height(8.dp)
                    .background(colors.warning.copy(alpha = 0.8f), MaterialTheme.shapes.small)
            )
        }
    }
}
