package com.johnpaulpatigas.weightcast.domain.trend.model

/**
 * Diagnostics detailing the subwindow analysis and residual dispersion for trend stability.
 *
 * @property residualStdDevKg Standard deviation of model residuals in KG.
 * @property firstHalfSlopeKgPerWeek Subwindow slope for first chronological half in KG/week, or null if < 2 distinct points.
 * @property secondHalfSlopeKgPerWeek Subwindow slope for second chronological half in KG/week, or null if < 2 distinct points.
 * @property fullWindowSlopeKgPerWeek Full-window slope in KG/week.
 * @property slopeDifferenceKgPerWeek Absolute difference between subwindow slopes in KG/week, or null if unavailable.
 * @property isDirectionReversal True if subwindows show significant opposite slope directions (reversal).
 * @property stabilityStatusMessage Description of stability evaluation findings.
 */
data class TrendStabilityDiagnostics(
    val residualStdDevKg: Double,
    val firstHalfSlopeKgPerWeek: Double?,
    val secondHalfSlopeKgPerWeek: Double?,
    val fullWindowSlopeKgPerWeek: Double,
    val slopeDifferenceKgPerWeek: Double?,
    val isDirectionReversal: Boolean,
    val stabilityStatusMessage: String
)
