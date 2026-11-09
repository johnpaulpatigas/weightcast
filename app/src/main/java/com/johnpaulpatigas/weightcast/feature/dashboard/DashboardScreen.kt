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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.DashboardSummary
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.feature.dashboard.components.WeightTrendChart
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightEntryItem

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
                title = { Text("WeightCast Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQuickAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add Weight")
            }
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
            // 1. Primary Weight Metric Card
            item {
                CurrentWeightOverviewCard(
                    summary = uiState.summary,
                    displayUnit = uiState.displayUnit
                )
            }

            // 2. Goal Progress Card (if set)
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

            // 4. Weight Trend Chart
            item {
                WeightTrendChart(
                    chartPoints = uiState.chartPoints,
                    displayUnit = uiState.displayUnit
                )
            }

            // 5. Recent Weight Entries
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
                    Text(
                        text = "No weight logs recorded yet. Tap + to add your first entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Current Weight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            val currentStr = summary.currentWeightInKg?.let {
                WeightConverter.formatDisplay(it, displayUnit, 1)
            } ?: "No data"

            Text(
                text = currentStr,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val trendStr = summary.smoothedTrendWeightInKg?.let {
                    "Trend: ${WeightConverter.formatDisplay(it, displayUnit, 1)}"
                } ?: "Trend: N/A"

                Text(
                    text = trendStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                summary.weightChange7dInKg?.let { changeKg ->
                    Spacer(modifier = Modifier.width(16.dp))

                    val convertedChange = WeightConverter.fromCanonicalKg(changeKg, displayUnit)
                    val roundedChange = WeightConverter.roundTo(convertedChange, 1)
                    val isLoss = roundedChange < 0
                    val changeStr = "${if (roundedChange > 0) "+" else ""}$roundedChange ${displayUnit.symbol} (7d)"

                    Icon(
                        imageVector = if (isLoss) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (isLoss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.height(18.dp).width(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = changeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            summary.targetProgressPercentage?.let { progress ->
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            summary.estimatedTargetDateMillis?.let { targetDate ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Est. Target Date: ${DateTimeUtils.formatDate(targetDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
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
                label = { Text(range.label) }
            )
        }
    }
}
