package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailabilityStatus
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastResult
import com.johnpaulpatigas.weightcast.domain.forecast.model.WeightForecast
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate

object ForecastingEngine {

    /**
     * Generates statistical weight forecasts for supported horizons using the fitted Theil-Sen regression estimate
     * and residual bootstrap prediction intervals.
     *
     * @param preparedMeasurements Daily aggregated measurements sorted by timestamp.
     * @param estimate Historical Theil-Sen trend regression estimate.
     * @param dataSufficiency Evaluated data sufficiency classification.
     * @param trendStability Evaluated trend stability classification.
     * @param evaluationTimestamp Target forecast origin timestamp in epoch millis.
     * @param horizons Supported forecast horizons (defaults to 7, 30, and 90 days).
     * @param bootstrapIterations Number of iterations for residual bootstrap interval calculation (default 500).
     * @param randomSeed Optional random seed for reproducible bootstrap sampling.
     * @return [ForecastResult] encapsulating point forecasts, prediction intervals, and availability metadata.
     */
    fun generateForecasts(
        preparedMeasurements: List<PreparedMeasurement>,
        estimate: WeightTrendEstimate?,
        dataSufficiency: DataSufficiency,
        trendStability: TrendStability,
        evaluationTimestamp: Long = System.currentTimeMillis(),
        horizons: List<ForecastHorizon> = ForecastHorizon.entries,
        bootstrapIterations: Int = 500,
        randomSeed: Long? = null
    ): ForecastResult {
        val overallAvailability = ForecastEligibilityEvaluator.evaluateOverall(
            dataSufficiency = dataSufficiency,
            trendStability = trendStability,
            estimate = estimate
        )

        val forecastMap = mutableMapOf<ForecastHorizon, WeightForecast>()

        val baselineTimestamp = preparedMeasurements.firstOrNull()?.timestamp ?: evaluationTimestamp
        val evalElapsedDays = (evaluationTimestamp - baselineTimestamp) / DataPreparation.MILLIS_PER_DAY

        for (horizon in horizons) {
            val availability = ForecastEligibilityEvaluator.evaluate(
                dataSufficiency = dataSufficiency,
                trendStability = trendStability,
                estimate = estimate,
                horizon = horizon
            )

            val targetTimestamp = evaluationTimestamp + (horizon.days * DataPreparation.MILLIS_PER_DAY).toLong()

            if (availability.isAvailable && estimate != null) {
                val targetElapsedDays = evalElapsedDays + horizon.days
                val pointEstimate = estimate.interceptKg + (estimate.slopeKgPerDay * targetElapsedDays)

                if (pointEstimate < 0.0 || pointEstimate.isNaN() || pointEstimate.isInfinite()) {
                    forecastMap[horizon] = WeightForecast(
                        horizon = horizon,
                        targetTimestamp = targetTimestamp,
                        predictedWeightKg = null,
                        predictionInterval = null,
                        availability = ForecastAvailability(
                            status = ForecastAvailabilityStatus.PHYSICAL_CONSTRAINT_VIOLATION,
                            reason = "Predicted weight is non-positive or non-finite."
                        )
                    )
                } else {
                    val interval = PredictionIntervalEstimator.estimate(
                        preparedMeasurements = preparedMeasurements,
                        estimate = estimate,
                        evaluationTimestamp = evaluationTimestamp,
                        horizonDays = horizon.days.toDouble(),
                        bootstrapIterations = bootstrapIterations,
                        randomSeed = randomSeed
                    )

                    forecastMap[horizon] = WeightForecast(
                        horizon = horizon,
                        targetTimestamp = targetTimestamp,
                        predictedWeightKg = pointEstimate,
                        predictionInterval = interval,
                        availability = availability
                    )
                }
            } else {
                forecastMap[horizon] = WeightForecast(
                    horizon = horizon,
                    targetTimestamp = targetTimestamp,
                    predictedWeightKg = null,
                    predictionInterval = null,
                    availability = availability
                )
            }
        }

        return ForecastResult(
            forecastOriginTimestamp = evaluationTimestamp,
            forecasts = forecastMap,
            dataSufficiency = dataSufficiency,
            trendStability = trendStability,
            overallAvailability = overallAvailability
        )
    }
}
