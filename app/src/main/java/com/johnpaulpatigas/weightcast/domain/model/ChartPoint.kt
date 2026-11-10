package com.johnpaulpatigas.weightcast.domain.model

/**
 * Data point for plotting historical weight charts, trend lines, and future forecast trajectories.
 */
data class ChartPoint(
    val timestamp: Long,
    val actualWeightInKg: Double?,
    val smoothedTrendInKg: Double?,
    val forecastWeightInKg: Double? = null,
    val lowerBoundInKg: Double? = null,
    val upperBoundInKg: Double? = null
)
