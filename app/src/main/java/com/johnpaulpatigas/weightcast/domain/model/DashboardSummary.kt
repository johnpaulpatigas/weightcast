package com.johnpaulpatigas.weightcast.domain.model

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastResult
import com.johnpaulpatigas.weightcast.domain.forecast.model.TargetAchievementEstimate
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendDiagnostics
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate

/**
 * Encapsulates aggregated metrics displayed on the Dashboard screen.
 */
data class DashboardSummary(
    val currentWeightInKg: Double? = null,
    val smoothedTrendWeightInKg: Double? = null,
    val weeklyChangeKg: Double? = null,
    val trendEstimate: WeightTrendEstimate? = null,
    val dataSufficiency: DataSufficiency = DataSufficiency.INSUFFICIENT_DATA,
    val trendStability: TrendStability = TrendStability.UNSTABLE,
    val trendDiagnostics: TrendDiagnostics? = null,
    val targetGoal: WeightGoal? = null,
    val targetProgressPercentage: Float? = null,
    val estimatedTargetDateMillis: Long? = null,
    val forecastResult: ForecastResult? = null,
    val targetAchievementEstimate: TargetAchievementEstimate? = null,
    val recentEntries: List<WeightEntry> = emptyList()
)
