package com.johnpaulpatigas.weightcast.domain.trend.model

/**
 * Immutable statistical estimate of a body weight trend.
 *
 * @property currentTrendKg Estimated trend weight in canonical Kilograms (KG) at current evaluation time.
 * @property slopeKgPerDay Daily rate of weight change in Kilograms per day (KG/day).
 * @property interceptKg Regression intercept parameter (b) in Kilograms (KG) at reference time t0.
 * @property measurementCount Number of valid measurements included in estimation window.
 * @property timeSpanDays Total calendar time span covered by measurements in days.
 * @property dataSufficiency Classification of data sufficiency for this estimate.
 * @property trendStability Classification of trend stability and residual consistency.
 * @property residualStdDevKg Standard deviation of model residuals (actual - trend) in Kilograms (KG).
 */
data class WeightTrendEstimate(
    val currentTrendKg: Double,
    val slopeKgPerDay: Double,
    val interceptKg: Double,
    val measurementCount: Int,
    val timeSpanDays: Double,
    val dataSufficiency: DataSufficiency,
    val trendStability: TrendStability,
    val residualStdDevKg: Double
) {
    /**
     * Derived weekly rate of weight change in Kilograms per week (KG/week).
     */
    val weeklyChangeKg: Double
        get() = slopeKgPerDay * 7.0

    init {
        require(!currentTrendKg.isNaN() && !currentTrendKg.isInfinite() && currentTrendKg >= 0.0) {
            "currentTrendKg must be a non-negative finite number, got $currentTrendKg"
        }
        require(!slopeKgPerDay.isNaN() && !slopeKgPerDay.isInfinite()) {
            "slopeKgPerDay must be a finite number, got $slopeKgPerDay"
        }
        require(!interceptKg.isNaN() && !interceptKg.isInfinite()) {
            "interceptKg must be a finite number, got $interceptKg"
        }
        require(measurementCount >= 0) {
            "measurementCount cannot be negative, got $measurementCount"
        }
        require(!timeSpanDays.isNaN() && !timeSpanDays.isInfinite() && timeSpanDays >= 0.0) {
            "timeSpanDays must be a non-negative finite number, got $timeSpanDays"
        }
        require(!residualStdDevKg.isNaN() && !residualStdDevKg.isInfinite() && residualStdDevKg >= 0.0) {
            "residualStdDevKg must be a non-negative finite number, got $residualStdDevKg"
        }
    }
}
