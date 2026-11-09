package com.johnpaulpatigas.weightcast.feature.dashboard

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.DashboardSummary
import com.johnpaulpatigas.weightcast.domain.model.TimeRange

data class DashboardUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val displayUnit: WeightUnit = WeightUnit.KG,
    val selectedTimeRange: TimeRange = TimeRange.DAYS_30,
    val chartPoints: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = true
)
