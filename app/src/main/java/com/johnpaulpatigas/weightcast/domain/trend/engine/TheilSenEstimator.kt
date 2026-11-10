package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import kotlin.math.sqrt

/**
 * Pure Kotlin implementation of the Theil-Sen robust linear regression estimator.
 *
 * Mathematical Definition:
 * For measurements (t_i, w_i) in elapsed days and canonical KG:
 * 1. Calculate pairwise slopes: s_ij = (w_j - w_i) / (t_j - t_i) for all t_j > t_i.
 * 2. Slope beta1 = median(s_ij).
 * 3. Intercept beta0 = median(w_i - beta1 * t_i).
 * 4. Fitted trend weight at t_eval: W_trend(t_eval) = beta0 + beta1 * t_eval.
 * 5. Residuals: e_i = w_i - (beta0 + beta1 * t_i).
 * 6. Residual Std Dev: s_e = sqrt( sum(e_i^2) / (n - 2) ) for n > 2, else 0.0.
 *    (Descriptive residual-scatter statistic around the fitted line; not a confidence or prediction interval).
 */
object TheilSenEstimator {

    private const val MILLIS_PER_DAY = 86_400_000.0

    /**
     * Computes the robust Theil-Sen trend regression estimate from prepared measurements.
     *
     * @param preparedMeasurements Prepared, daily-aggregated measurements sorted by timestamp.
     * @param evaluationTimestamp Target epoch timestamp in millis at which to calculate current trend weight.
     * @param dataSufficiency Pre-evaluated or default sufficiency classification.
     * @param trendStability Pre-evaluated or default stability classification.
     * @return [WeightTrendEstimate] or null if insufficient measurements (< 2), no valid slopes, or negative/non-finite fitted trend weight.
     */
    fun estimate(
        preparedMeasurements: List<PreparedMeasurement>,
        evaluationTimestamp: Long = System.currentTimeMillis(),
        dataSufficiency: DataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
        trendStability: TrendStability = TrendStability.STABLE
    ): WeightTrendEstimate? {
        if (preparedMeasurements.size < 2) return null

        val sorted = preparedMeasurements.sortedBy { it.timestamp }
        val n = sorted.size

        val baselineTimestamp = sorted.first().timestamp

        val maxPairs = n * (n - 1) / 2
        val slopes = DoubleArray(maxPairs)
        var slopeCount = 0

        for (i in 0 until n - 1) {
            val ti = sorted[i].elapsedDays
            val wi = sorted[i].weightKg

            for (j in i + 1 until n) {
                val tj = sorted[j].elapsedDays
                val wj = sorted[j].weightKg

                val dt = tj - ti
                if (dt > 0.0) {
                    val slope = (wj - wi) / dt
                    if (!slope.isNaN() && !slope.isInfinite()) {
                        slopes[slopeCount++] = slope
                    }
                }
            }
        }

        if (slopeCount == 0) return null

        val beta1 = calculateMedian(slopes, slopeCount)

        val intercepts = DoubleArray(n)
        for (i in 0 until n) {
            intercepts[i] = sorted[i].weightKg - (beta1 * sorted[i].elapsedDays)
        }
        val beta0 = calculateMedian(intercepts, n)

        val evalElapsedDays = (evaluationTimestamp - baselineTimestamp) / MILLIS_PER_DAY
        val fittedCurrentTrendKg = beta0 + (beta1 * evalElapsedDays)

        // Reject negative or non-finite fitted current trend weight (do not silently clamp to zero)
        if (fittedCurrentTrendKg < 0.0 || fittedCurrentTrendKg.isNaN() || fittedCurrentTrendKg.isInfinite()) {
            return null
        }

        var sumSquaredResiduals = 0.0
        for (i in 0 until n) {
            val fitted = beta0 + (beta1 * sorted[i].elapsedDays)
            val residual = sorted[i].weightKg - fitted
            sumSquaredResiduals += residual * residual
        }

        val residualStdDevKg = if (n > 2) {
            sqrt(sumSquaredResiduals / (n - 2))
        } else {
            0.0
        }

        val timeSpanDays = sorted.last().elapsedDays - sorted.first().elapsedDays

        return WeightTrendEstimate(
            currentTrendKg = fittedCurrentTrendKg,
            slopeKgPerDay = beta1,
            interceptKg = beta0,
            measurementCount = n,
            timeSpanDays = timeSpanDays,
            dataSufficiency = dataSufficiency,
            trendStability = trendStability,
            residualStdDevKg = residualStdDevKg
        )
    }

    /**
     * Calculates the median of a double array slice [0 until count].
     * Handles even count by averaging the two middle elements.
     */
    fun calculateMedian(array: DoubleArray, count: Int): Double {
        require(count > 0) { "Cannot calculate median of empty array slice" }
        array.sort(0, count)
        val mid = count / 2
        return if (count % 2 == 1) {
            array[mid]
        } else {
            (array[mid - 1] + array[mid]) / 2.0
        }
    }
}
