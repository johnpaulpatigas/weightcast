package com.johnpaulpatigas.weightcast.feature.weight_history

import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

data class WeightHistoryUiState(
    val selectedTimeRange: TimeRange = TimeRange.MONTHS_1,
    val chartPoints: List<ChartPoint> = emptyList(),
    val allEntries: List<WeightEntry> = emptyList(),
    val filteredEntries: List<WeightEntry> = emptyList(),
    val isLoading: Boolean = true
)
