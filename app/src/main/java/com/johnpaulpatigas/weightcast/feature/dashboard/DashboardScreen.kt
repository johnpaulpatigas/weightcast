package com.johnpaulpatigas.weightcast.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.DashboardSummary
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.feature.dashboard.components.WeightTrendChart
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightEntryItem
import com.johnpaulpatigas.weightcast.ui.motion.AnimatedMetricContent
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WeightCast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onQuickAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Weight", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Primary Weight Metric & Trend Overview Card
            item {
                CurrentWeightOverviewCard(
                    summary = uiState.summary,
                    displayUnit = uiState.displayUnit
                )
            }

            // 2. Target Goal Progress Card (if set)
            if (uiState.summary.targetGoal != null) {
                item {
                    TargetGoalCard(
                        summary = uiState.summary,
                        displayUnit = uiState.displayUnit
                    )
                }
            }

            // 3. Time Range Filter Chips
            item {
                TimeRangeChipRow(
                    selectedRange = uiState.selectedTimeRange,
                    onRangeSelected = { viewModel.onTimeRangeSelected(it) }
                )
            }

            // 4. Interactive Weight Trend Chart
            item {
                WeightTrendChart(
                    chartPoints = uiState.chartPoints,
                    displayUnit = uiState.displayUnit
                )
            }

            // 5. Recent Weight Entries List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.summary.recentEntries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                modifier = Modifier.height(48.dp).width(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No entries recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.summary.recentEntries,
                    key = { it.id }
                ) { entry ->
                    WeightEntryItem(
                        entry = entry,
                        displayUnit = uiState.displayUnit,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentWeightOverviewCard(
    summary: DashboardSummary,
    displayUnit: WeightUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recorded Weight",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val latestDate = summary.recentEntries.firstOrNull()?.timestamp
                if (latestDate != null) {
                    Text(
                        text = DateTimeUtils.formatDate(latestDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Primary: Authoritative Recorded Weight
            val currentStr = summary.currentWeightInKg?.let {
                WeightConverter.formatDisplay(it, displayUnit, 1)
            } ?: "No data"

            AnimatedMetricContent(targetState = currentStr) { weightText ->
                Text(
                    text = weightText,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary: Statistical Trend & Status Badges
            val isSufficient = summary.dataSufficiency != DataSufficiency.INSUFFICIENT_DATA

            if (isSufficient && summary.smoothedTrendWeightInKg != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trendStr = "Trend: ${WeightConverter.formatDisplay(summary.smoothedTrendWeightInKg, displayUnit, 1)}"

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = trendStr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    summary.weeklyChangeKg?.let { weeklyChange ->
                        Spacer(modifier = Modifier.width(12.dp))

                        val convertedChange = WeightConverter.fromCanonicalKg(weeklyChange, displayUnit)
                        val roundedChange = WeightConverter.roundTo(convertedChange, 1)
                        val absChange = abs(roundedChange)
                        val isLoss = roundedChange < 0
                        val directionWord = if (isLoss) "decreasing" else if (roundedChange > 0) "increasing" else "unchanged"
                        val changeText = "$directionWord (${if (roundedChange > 0) "+" else ""}$roundedChange ${displayUnit.symbol}/wk)"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics { contentDescription = changeText }
                        ) {
                            Icon(
                                imageVector = if (isLoss) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (isLoss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.height(18.dp).width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = changeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sufficiency & Stability Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sufficiencyLabel = when (summary.dataSufficiency) {
                        DataSufficiency.HIGH_CONFIDENCE -> "Sufficient data"
                        DataSufficiency.MODERATE_CONFIDENCE -> "Moderate data"
                        DataSufficiency.LOW_CONFIDENCE -> "Limited data"
                        DataSufficiency.INSUFFICIENT_DATA -> "Not enough data"
                    }

                    val stabilityLabel = when (summary.trendStability) {
                        TrendStability.STABLE -> "Stable trend"
                        TrendStability.MODERATE -> "Variable trend"
                        TrendStability.UNSTABLE -> "Unstable trend"
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = sufficiencyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stabilityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Not enough data to estimate statistical trend",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetGoalCard(
    summary: DashboardSummary,
    displayUnit: WeightUnit
) {
    val goal = summary.targetGoal ?: return
    val currentKg = summary.currentWeightInKg

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Goal: ${goal.goalType.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Target: ${WeightConverter.formatDisplay(goal.targetWeightInKg, displayUnit)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (currentKg != null) {
                val remainingKg = abs(goal.targetWeightInKg - currentKg)
                val remainingStr = WeightConverter.formatDisplay(remainingKg, displayUnit)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$remainingStr remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            summary.targetProgressPercentage?.let { progress ->
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeChipRow(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TimeRange.entries.toTypedArray()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label, fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Normal) },
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
