package com.johnpaulpatigas.weightcast.domain.model

/**
 * Data point for plotting weight charts.
 */
data class ChartPoint(
    val timestamp: Long,
    val actualWeightInKg: Double?,
    val smoothedTrendInKg: Double?
)
