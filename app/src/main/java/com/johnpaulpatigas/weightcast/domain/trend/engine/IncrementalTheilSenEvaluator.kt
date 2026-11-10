package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

/**
 * High-performance exact incremental historical trend evaluator.
 *
 * For a sequence of N historical measurements, this evaluator computes exact Theil-Sen regression
 * estimates at every historical timestamp in O(N^2 log N) total time (compared to O(N^3 log N)
 * for independent baseline evaluation).
 *
 * Mathematical Equivalence:
 * Guarantees 100% mathematical equivalence to independently evaluating [StatisticalTrendEvaluator.evaluateTrend]
 * for every historical prefix up to each evaluation timestamp:
 * 1. Preserves UTC daily aggregation (using arithmetic mean of daily valid weights).
 * 2. Preserves baseline timestamp t0 normalization (relative to first valid UTC day).
 * 3. Preserves timestamp coordinate system and elapsed days calculations.
 * 4. Preserves dynamic median slope computation (via dual-heap tracker).
 * 5. Preserves median intercept calculation for each historical prefix.
 * 6. Preserves data sufficiency gating (requires at least 3 distinct daily measurements).
 * 7. Preserves negative and non-finite fitted trend weight rejection.
 */
object IncrementalTheilSenEvaluator {

    /**
     * Calculates historical chart data points for [allEntries], mapping each target entry to its
     * exact statistical trend weight evaluated using all measurements available on or before its timestamp.
     *
     * @param allEntries Full set of raw weight entries available in the repository.
     * @param targetEntries Subset of entries (e.g. filtered by TimeRange) for which to generate chart points.
     * @return List of [ChartPoint]s with exact smoothed trend weights.
     */
    fun evaluateHistoricalChartPoints(
        allEntries: List<WeightEntry>,
        targetEntries: List<WeightEntry>
    ): List<ChartPoint> {
        if (targetEntries.isEmpty() || allEntries.isEmpty()) return emptyList()

        val targetTimestamps = targetEntries.map { it.timestamp }.toSet()

        val trendMap = evaluateHistoricalTrendMap(
            allEntries = allEntries,
            evalTimestamps = targetTimestamps
        )

        return targetEntries.map { entry ->
            ChartPoint(
                timestamp = entry.timestamp,
                actualWeightInKg = entry.weightInKg,
                smoothedTrendInKg = trendMap[entry.timestamp]
            )
        }
    }

    /**
     * Evaluates exact statistical trend weights at each timestamp in [evalTimestamps] using
     * all measurements available on or before that timestamp.
     */
    fun evaluateHistoricalTrendMap(
        allEntries: List<WeightEntry>,
        evalTimestamps: Set<Long>
    ): Map<Long, Double?> {
        if (allEntries.isEmpty() || evalTimestamps.isEmpty()) return emptyMap()

        val sortedRaw = allEntries.sortedBy { it.timestamp }
        val result = mutableMapOf<Long, Double?>()

        val dailyRepresentatives = mutableListOf<PreparedMeasurement>()
        val slopeTracker = DoubleMedianTracker()
        var interceptBuffer = DoubleArray(64)

        var lastProcessedTimestamp: Long? = null
        val currentPrefixValidRaw = mutableListOf<WeightEntry>()

        val sortedEvalTimestamps = evalTimestamps.sorted()
        var rawIndex = 0

        for (evalTime in sortedEvalTimestamps) {
            var rawAddedOrChanged = false

            while (rawIndex < sortedRaw.size && sortedRaw[rawIndex].timestamp <= evalTime) {
                currentPrefixValidRaw.add(sortedRaw[rawIndex])
                rawIndex++
                rawAddedOrChanged = true
            }

            if (!rawAddedOrChanged && lastProcessedTimestamp != null) {
                result[evalTime] = result[lastProcessedTimestamp]
                continue
            }

            lastProcessedTimestamp = evalTime

            val prepared = DataPreparation.prepare(currentPrefixValidRaw)
            val measurements = prepared.measurements

            if (measurements.size < 3) {
                result[evalTime] = null
                continue
            }

            val n = measurements.size
            val prevCount = dailyRepresentatives.size

            if (prevCount == 0) {
                for (i in 0 until n) {
                    dailyRepresentatives.add(measurements[i])
                    val ti = measurements[i].elapsedDays
                    val wi = measurements[i].weightKg
                    for (j in 0 until i) {
                        val tj = measurements[j].elapsedDays
                        val wj = measurements[j].weightKg
                        val dt = ti - tj
                        if (dt > 0.0) {
                            val slope = (wi - wj) / dt
                            slopeTracker.add(slope)
                        }
                    }
                }
            } else if (n == prevCount && measurements.subList(0, prevCount) == dailyRepresentatives) {
                // No change in daily representatives
            } else if (n == prevCount + 1 && measurements.subList(0, prevCount) == dailyRepresentatives) {
                val newMeasurement = measurements.last()
                dailyRepresentatives.add(newMeasurement)
                val ti = newMeasurement.elapsedDays
                val wi = newMeasurement.weightKg

                for (j in 0 until prevCount) {
                    val tj = dailyRepresentatives[j].elapsedDays
                    val wj = dailyRepresentatives[j].weightKg
                    val dt = ti - tj
                    if (dt > 0.0) {
                        val slope = (wi - wj) / dt
                        slopeTracker.add(slope)
                    }
                }
            } else {
                dailyRepresentatives.clear()
                slopeTracker.clear()
                dailyRepresentatives.addAll(measurements)

                for (i in 0 until n) {
                    val ti = measurements[i].elapsedDays
                    val wi = measurements[i].weightKg
                    for (j in 0 until i) {
                        val tj = measurements[j].elapsedDays
                        val wj = measurements[j].weightKg
                        val dt = ti - tj
                        if (dt > 0.0) {
                            val slope = (wi - wj) / dt
                            slopeTracker.add(slope)
                        }
                    }
                }
            }

            if (slopeTracker.size == 0) {
                result[evalTime] = null
                continue
            }

            val beta1 = slopeTracker.getMedian()

            if (interceptBuffer.size < n) {
                interceptBuffer = DoubleArray(n * 2)
            }
            for (i in 0 until n) {
                interceptBuffer[i] = measurements[i].weightKg - (beta1 * measurements[i].elapsedDays)
            }
            val beta0 = TheilSenEstimator.calculateMedian(interceptBuffer, n)

            val baselineTimestamp = measurements.first().timestamp
            val evalElapsedDays = (evalTime - baselineTimestamp) / DataPreparation.MILLIS_PER_DAY
            val fitted = beta0 + (beta1 * evalElapsedDays)

            if (fitted < 0.0 || fitted.isNaN() || fitted.isInfinite()) {
                result[evalTime] = null
            } else {
                result[evalTime] = fitted
            }
        }

        return result
    }
}
