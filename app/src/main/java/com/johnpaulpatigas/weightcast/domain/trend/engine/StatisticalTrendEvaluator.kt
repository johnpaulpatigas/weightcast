package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendDiagnostics
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendEstimationResult

object StatisticalTrendEvaluator {

    /**
     * Complete pipeline orchestrating measurement preparation, data sufficiency evaluation,
     * Theil-Sen regression estimation, and trend stability diagnostics.
     *
     * @param rawEntries Raw list of user weight entries.
     * @param evaluationTimestamp Target epoch timestamp in millis (defaults to latest measurement or System time).
     */
    fun evaluateTrend(
        rawEntries: List<WeightEntry>,
        evaluationTimestamp: Long = System.currentTimeMillis()
    ): TrendEstimationResult {
        // 1. Data Preparation & Daily Aggregation
        val preparedDataset = DataPreparation.prepare(rawEntries)
        val allMeasurements = preparedDataset.measurements
        val prepDiagnostics = preparedDataset.diagnostics

        // Filter measurements on or before evaluation timestamp to prevent future-data leakage
        val measurements = allMeasurements.filter { it.timestamp <= evaluationTimestamp }

        // 2. Data Sufficiency Evaluation
        val sufficiency = DataSufficiencyEvaluator.evaluate(measurements, evaluationTimestamp)

        if (sufficiency == DataSufficiency.INSUFFICIENT_DATA || measurements.size < 2) {
            val timeSpanDays = if (measurements.isNotEmpty()) {
                (measurements.last().timestamp - measurements.first().timestamp) / DataPreparation.MILLIS_PER_DAY
            } else 0.0

            val totalCalendarDays = (timeSpanDays.toInt()) + 1
            val coverage = if (measurements.isNotEmpty()) measurements.size.toDouble() / totalCalendarDays.toDouble() else 0.0

            val emptyDiagnostics = TrendDiagnostics(
                validMeasurementCount = prepDiagnostics.acceptedRawCount,
                rejectedMeasurementCount = prepDiagnostics.rejectedRawCount,
                timeSpanDays = timeSpanDays,
                coverageRatio = coverage,
                validPairwiseSlopeCount = 0,
                stabilityDiagnostics = null,
                statusMessage = "Insufficient data: at least 3 distinct daily measurements are required to estimate a trend."
            )

            return TrendEstimationResult(
                estimate = null,
                sufficiency = sufficiency,
                diagnostics = emptyDiagnostics
            )
        }

        // 3. Raw Theil-Sen Regression Estimation
        val initialEstimate = TheilSenEstimator.estimate(
            preparedMeasurements = measurements,
            evaluationTimestamp = evaluationTimestamp,
            dataSufficiency = sufficiency
        )

        if (initialEstimate == null) {
            val timeSpanDays = measurements.last().elapsedDays - measurements.first().elapsedDays
            val totalCalendarDays = (timeSpanDays.toInt()) + 1
            val coverage = measurements.size.toDouble() / totalCalendarDays.toDouble()

            val failedDiagnostics = TrendDiagnostics(
                validMeasurementCount = prepDiagnostics.acceptedRawCount,
                rejectedMeasurementCount = prepDiagnostics.rejectedRawCount,
                timeSpanDays = timeSpanDays,
                coverageRatio = coverage,
                validPairwiseSlopeCount = 0,
                stabilityDiagnostics = null,
                statusMessage = "Unable to compute valid slope from measurements."
            )

            return TrendEstimationResult(
                estimate = null,
                sufficiency = DataSufficiency.INSUFFICIENT_DATA,
                diagnostics = failedDiagnostics
            )
        }

        // 4. Trend Stability Evaluation
        val (stability, stabilityDiagnostics) = TrendStabilityEvaluator.evaluate(measurements, initialEstimate)

        // 5. Final Estimate with evaluated sufficiency and stability
        val finalEstimate = initialEstimate.copy(
            dataSufficiency = sufficiency,
            trendStability = stability
        )

        val n = measurements.size
        val timeSpanDays = measurements.last().elapsedDays - measurements.first().elapsedDays
        val totalCalendarDays = (timeSpanDays.toInt()) + 1
        val coverageRatio = n.toDouble() / totalCalendarDays.toDouble()
        val pairwiseSlopeCount = n * (n - 1) / 2

        val fullDiagnostics = TrendDiagnostics(
            validMeasurementCount = prepDiagnostics.acceptedRawCount,
            rejectedMeasurementCount = prepDiagnostics.rejectedRawCount,
            timeSpanDays = timeSpanDays,
            coverageRatio = coverageRatio,
            validPairwiseSlopeCount = pairwiseSlopeCount,
            stabilityDiagnostics = stabilityDiagnostics,
            statusMessage = "Trend estimated with ${sufficiency.displayName} and ${stability.displayName} trajectory."
        )

        return TrendEstimationResult(
            estimate = finalEstimate,
            sufficiency = sufficiency,
            diagnostics = fullDiagnostics
        )
    }
}
