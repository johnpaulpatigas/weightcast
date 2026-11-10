package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.PredictionInterval
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement
import com.johnpaulpatigas.weightcast.domain.trend.engine.TheilSenEstimator
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object PredictionIntervalEstimator {

    /**
     * Estimates non-parametric prediction intervals for a forecast horizon using a residual bootstrap with refitting.
     *
     * @param preparedMeasurements Prepared daily measurement representatives sorted by timestamp.
     * @param estimate Historical fitted trend estimate.
     * @param evaluationTimestamp Forecast origin timestamp in epoch millis.
     * @param horizonDays Forecast horizon in days (e.g. 7.0, 30.0, 90.0).
     * @param confidenceLevel Target confidence level (default 0.95 for 95% interval).
     * @param bootstrapIterations Number of bootstrap iterations (default 500).
     * @param randomSeed Optional random seed for deterministic test executions.
     * @return Validated [PredictionInterval] or null if computation fails or interval is non-finite.
     */
    fun estimate(
        preparedMeasurements: List<PreparedMeasurement>,
        estimate: WeightTrendEstimate,
        evaluationTimestamp: Long,
        horizonDays: Double,
        confidenceLevel: Double = 0.95,
        bootstrapIterations: Int = 500,
        randomSeed: Long? = null
    ): PredictionInterval? {
        if (preparedMeasurements.size < 2 || bootstrapIterations < 10) return null
        if (estimate.slopeKgPerDay.isNaN() || estimate.slopeKgPerDay.isInfinite()) return null

        val sorted = preparedMeasurements.sortedBy { it.timestamp }
        val n = sorted.size
        val baselineTs = sorted.first().timestamp

        val evalDays = (evaluationTimestamp - baselineTs) / DataPreparation.MILLIS_PER_DAY
        val targetDays = evalDays + horizonDays

        val pointEstimateKg = estimate.interceptKg + (estimate.slopeKgPerDay * targetDays)
        if (pointEstimateKg < 0.0 || pointEstimateKg.isNaN() || pointEstimateKg.isInfinite()) return null

        // 1. Calculate historical residuals e_i = w_i - (beta0 + beta1 * t_i)
        val residuals = DoubleArray(n)
        var sumResiduals = 0.0
        for (i in 0 until n) {
            val fitted = estimate.interceptKg + (estimate.slopeKgPerDay * sorted[i].elapsedDays)
            residuals[i] = sorted[i].weightKg - fitted
            sumResiduals += residuals[i]
        }
        val meanResidual = sumResiduals / n

        // Mean-center residuals
        val centeredResiduals = DoubleArray(n) { i -> residuals[i] - meanResidual }

        val random = randomSeed?.let { Random(it) } ?: Random.Default

        val maxPairs = n * (n - 1) / 2
        val slopesBuffer = DoubleArray(maxPairs)
        val interceptsBuffer = DoubleArray(n)
        val syntheticWeights = DoubleArray(n)
        val predictions = DoubleArray(bootstrapIterations)

        // 2. Residual Bootstrap with Model Refitting
        for (b in 0 until bootstrapIterations) {
            // Construct synthetic observation w_i* = (beta0 + beta1 * t_i) + e_i*
            for (i in 0 until n) {
                val resIdx = random.nextInt(n)
                val eStar = centeredResiduals[resIdx]
                val fitted = estimate.interceptKg + (estimate.slopeKgPerDay * sorted[i].elapsedDays)
                syntheticWeights[i] = fitted + eStar
            }

            // Refit slope beta1* using pairwise slopes of synthetic dataset
            var slopeCount = 0
            for (i in 0 until n - 1) {
                val ti = sorted[i].elapsedDays
                val wi = syntheticWeights[i]
                for (j in i + 1 until n) {
                    val tj = sorted[j].elapsedDays
                    val wj = syntheticWeights[j]
                    val dt = tj - ti
                    if (dt > 0.0) {
                        val slope = (wj - wi) / dt
                        if (!slope.isNaN() && !slope.isInfinite()) {
                            slopesBuffer[slopeCount++] = slope
                        }
                    }
                }
            }

            val beta1Star = if (slopeCount > 0) {
                TheilSenEstimator.calculateMedian(slopesBuffer, slopeCount)
            } else {
                estimate.slopeKgPerDay
            }

            // Refit intercept beta0*
            for (i in 0 until n) {
                interceptsBuffer[i] = syntheticWeights[i] - (beta1Star * sorted[i].elapsedDays)
            }
            val beta0Star = TheilSenEstimator.calculateMedian(interceptsBuffer, n)

            // Future observation residual sampling e_future*
            val futureResIdx = random.nextInt(n)
            val eFutureStar = centeredResiduals[futureResIdx]

            val futurePred = beta0Star + (beta1Star * targetDays) + eFutureStar
            predictions[b] = futurePred
        }

        predictions.sort()

        val alpha = 1.0 - confidenceLevel
        val lowerIdx = max(0, min(bootstrapIterations - 1, (bootstrapIterations * (alpha / 2.0)).toInt()))
        val upperIdx = max(0, min(bootstrapIterations - 1, (bootstrapIterations * (1.0 - (alpha / 2.0))).toInt()))

        var lowerBound = max(0.0, predictions[lowerIdx])
        var upperBound = max(0.0, predictions[upperIdx])

        if (lowerBound.isNaN() || upperBound.isNaN() || lowerBound.isInfinite() || upperBound.isInfinite()) {
            return null
        }

        // Require lowerBound <= pointEstimate <= upperBound
        if (lowerBound > pointEstimateKg) lowerBound = pointEstimateKg
        if (upperBound < pointEstimateKg) upperBound = pointEstimateKg

        if (lowerBound > upperBound) return null

        return PredictionInterval(
            lowerBoundKg = lowerBound,
            upperBoundKg = upperBound,
            confidenceLevel = confidenceLevel
        )
    }
}
