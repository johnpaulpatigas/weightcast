package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import kotlin.math.max

object DataSufficiencyEvaluator {

    /**
     * Authoritative product thresholds for data sufficiency evaluation.
     */
    object Thresholds {
        const val MIN_MEASUREMENTS_FOR_LOW = 3
        const val MIN_MEASUREMENTS_FOR_MODERATE = 5
        const val MIN_MEASUREMENTS_FOR_HIGH = 10

        const val MIN_SPAN_DAYS_FOR_MODERATE = 7.0
        const val MIN_SPAN_DAYS_FOR_HIGH = 14.0

        const val MIN_COVERAGE_RATIO_FOR_MODERATE = 0.40
        const val MIN_COVERAGE_RATIO_FOR_HIGH = 0.50
    }

    /**
     * Evaluates the statistical data sufficiency of prepared measurements on or before [evaluationTimestamp].
     *
     * Coverage Ratio is a temporal measurement-density heuristic (ratio of daily measurements present over total calendar days in span).
     * It is not a calibrated probability of forecast accuracy.
     */
    fun evaluate(
        preparedMeasurements: List<PreparedMeasurement>,
        evaluationTimestamp: Long = Long.MAX_VALUE
    ): DataSufficiency {
        val validInWindow = preparedMeasurements.filter { it.timestamp <= evaluationTimestamp }
        if (validInWindow.isEmpty()) return DataSufficiency.INSUFFICIENT_DATA

        val sorted = validInWindow.sortedBy { it.timestamp }
        val count = sorted.size

        if (count < Thresholds.MIN_MEASUREMENTS_FOR_LOW) {
            return DataSufficiency.INSUFFICIENT_DATA
        }

        val firstTs = sorted.first().timestamp
        val lastTs = sorted.last().timestamp
        val timeSpanDays = max(0.0, (lastTs - firstTs) / DataPreparation.MILLIS_PER_DAY)
        val totalCalendarDaysInSpan = (timeSpanDays.toInt()) + 1

        val coverageRatio = count.toDouble() / totalCalendarDaysInSpan.toDouble()

        return when {
            count >= Thresholds.MIN_MEASUREMENTS_FOR_HIGH &&
                    timeSpanDays >= Thresholds.MIN_SPAN_DAYS_FOR_HIGH &&
                    coverageRatio >= Thresholds.MIN_COVERAGE_RATIO_FOR_HIGH -> DataSufficiency.HIGH_CONFIDENCE

            count >= Thresholds.MIN_MEASUREMENTS_FOR_MODERATE &&
                    timeSpanDays >= Thresholds.MIN_SPAN_DAYS_FOR_MODERATE &&
                    coverageRatio >= Thresholds.MIN_COVERAGE_RATIO_FOR_MODERATE -> DataSufficiency.MODERATE_CONFIDENCE

            else -> DataSufficiency.LOW_CONFIDENCE
        }
    }
}
