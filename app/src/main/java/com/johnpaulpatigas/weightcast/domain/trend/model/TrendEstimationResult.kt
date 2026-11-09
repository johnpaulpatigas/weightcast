package com.johnpaulpatigas.weightcast.domain.trend.model

/**
 * Diagnostic metadata providing full transparency into the trend estimation process.
 *
 * @property validMeasurementCount Count of valid measurements used.
 * @property rejectedMeasurementCount Count of rejected measurements (e.g. invalid/out-of-range).
 * @property timeSpanDays Total time span covered by measurements in days.
 * @property validPairwiseSlopeCount Number of valid non-zero time-delta pairs computed.
 * @property statusMessage Human-readable status description of the estimation result.
 */
data class TrendDiagnostics(
    val validMeasurementCount: Int,
    val rejectedMeasurementCount: Int,
    val timeSpanDays: Double,
    val validPairwiseSlopeCount: Int,
    val statusMessage: String
)

/**
 * Result wrapper encapsulating a weight trend estimate alongside sufficiency and diagnostics.
 *
 * @property estimate Calculated [WeightTrendEstimate], or null if estimation is impossible.
 * @property sufficiency Evaluated data sufficiency classification.
 * @property diagnostics Detailed diagnostic metadata regarding dataset analysis.
 */
data class TrendEstimationResult(
    val estimate: WeightTrendEstimate?,
    val sufficiency: DataSufficiency,
    val diagnostics: TrendDiagnostics
) {
    val isAvailable: Boolean
        get() = estimate != null && sufficiency != DataSufficiency.INSUFFICIENT_DATA
}
