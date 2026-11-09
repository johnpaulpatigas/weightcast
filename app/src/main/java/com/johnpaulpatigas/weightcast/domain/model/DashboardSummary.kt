package com.johnpaulpatigas.weightcast.domain.model

/**
 * Encapsulates aggregated metrics displayed on the Dashboard screen.
 */
data class DashboardSummary(
    val currentWeightInKg: Double? = null,
    val smoothedTrendWeightInKg: Double? = null,
    val weightChange7dInKg: Double? = null,
    val targetGoal: WeightGoal? = null,
    val targetProgressPercentage: Float? = null,
    val estimatedTargetDateMillis: Long? = null,
    val recentEntries: List<WeightEntry> = emptyList()
)
