package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailabilityStatus
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate

object ForecastEligibilityEvaluator {

    /**
     * Evaluates forecast eligibility for a specific horizon based on statistical sufficiency,
     * trend stability, and regression validity.
     */
    fun evaluate(
        dataSufficiency: DataSufficiency,
        trendStability: TrendStability,
        estimate: WeightTrendEstimate?,
        horizon: ForecastHorizon
    ): ForecastAvailability {
        if (estimate == null) {
            return ForecastAvailability(
                status = ForecastAvailabilityStatus.INSUFFICIENT_DATA,
                reason = "No valid statistical trend estimate available."
            )
        }

        if (estimate.currentTrendKg < 0.0 || estimate.slopeKgPerDay.isNaN() || estimate.slopeKgPerDay.isInfinite()) {
            return ForecastAvailability(
                status = ForecastAvailabilityStatus.INVALID_SLOPE,
                reason = "Non-finite or invalid slope parameters detected."
            )
        }

        return when (dataSufficiency) {
            DataSufficiency.INSUFFICIENT_DATA -> ForecastAvailability(
                status = ForecastAvailabilityStatus.INSUFFICIENT_DATA,
                reason = "Insufficient data: at least 3 distinct daily measurements required."
            )

            DataSufficiency.LOW_CONFIDENCE -> ForecastAvailability(
                status = ForecastAvailabilityStatus.LOW_CONFIDENCE,
                reason = "Low confidence: at least 7 days of logs and 5 measurements required for forecasting."
            )

            DataSufficiency.MODERATE_CONFIDENCE -> when (trendStability) {
                TrendStability.UNSTABLE -> ForecastAvailability(
                    status = ForecastAvailabilityStatus.UNSTABLE_TREND,
                    reason = "Unstable historical trend: forecasts suppressed due to trajectory changes or high scatter."
                )

                TrendStability.MODERATE -> if (horizon == ForecastHorizon.DAYS_7) {
                    ForecastAvailability(
                        status = ForecastAvailabilityStatus.AVAILABLE,
                        reason = "Moderate confidence with variable trend: 7-day forecast available."
                    )
                } else {
                    ForecastAvailability(
                        status = ForecastAvailabilityStatus.UNSTABLE_TREND,
                        reason = "Variable trend trajectory suppresses 30-day and 90-day forecasts."
                    )
                }

                TrendStability.STABLE -> if (horizon == ForecastHorizon.DAYS_7 || horizon == ForecastHorizon.DAYS_30) {
                    ForecastAvailability(
                        status = ForecastAvailabilityStatus.AVAILABLE,
                        reason = "Moderate confidence dataset: ${horizon.displayName} forecast available."
                    )
                } else {
                    ForecastAvailability(
                        status = ForecastAvailabilityStatus.LOW_CONFIDENCE,
                        reason = "90-day long-term forecast requires High Confidence data sufficiency."
                    )
                }
            }

            DataSufficiency.HIGH_CONFIDENCE -> when (trendStability) {
                TrendStability.UNSTABLE -> ForecastAvailability(
                    status = ForecastAvailabilityStatus.UNSTABLE_TREND,
                    reason = "Unstable historical trend: high noise or subwindow slope divergence."
                )

                TrendStability.MODERATE, TrendStability.STABLE -> ForecastAvailability(
                    status = ForecastAvailabilityStatus.AVAILABLE,
                    reason = "High confidence dataset: ${horizon.displayName} forecast available."
                )
            }
        }
    }

    /**
     * Evaluates overall dataset forecast eligibility (highest availability state across horizons).
     */
    fun evaluateOverall(
        dataSufficiency: DataSufficiency,
        trendStability: TrendStability,
        estimate: WeightTrendEstimate?
    ): ForecastAvailability {
        if (estimate == null) {
            return ForecastAvailability(
                status = ForecastAvailabilityStatus.INSUFFICIENT_DATA,
                reason = "Insufficient data to generate weight forecasts."
            )
        }

        val avail7 = evaluate(dataSufficiency, trendStability, estimate, ForecastHorizon.DAYS_7)
        if (avail7.isAvailable) {
            return ForecastAvailability(
                status = ForecastAvailabilityStatus.AVAILABLE,
                reason = "Forecasts available for current trend data."
            )
        }

        return avail7
    }
}
