package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStabilityDiagnostics
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import kotlin.math.abs

object TrendStabilityEvaluator {

    object Thresholds {
        const val MIN_WEEKLY_CHANGE_FOR_REVERSAL = 0.15
        const val SLOPE_DIFF_UNSTABLE_KG_PER_WEEK = 0.75
        const val SLOPE_DIFF_MODERATE_KG_PER_WEEK = 0.30

        const val RESIDUAL_STDDEV_UNSTABLE_KG = 1.2
        const val RESIDUAL_STDDEV_MODERATE_KG = 0.6
    }

    /**
     * Evaluates the consistency of the estimated weight trend across subwindows and residual dispersion.
     */
    fun evaluate(
        preparedMeasurements: List<PreparedMeasurement>,
        estimate: WeightTrendEstimate?
    ): Pair<TrendStability, TrendStabilityDiagnostics?> {
        if (estimate == null || preparedMeasurements.size < 2) {
            return Pair(TrendStability.UNSTABLE, null)
        }

        val sorted = preparedMeasurements.sortedBy { it.timestamp }
        val n = sorted.size
        val half = n / 2
        val evalTime = sorted.last().timestamp

        val firstHalf = sorted.take(half)
        val secondHalf = sorted.drop(half)

        val firstEstimate = if (firstHalf.size >= 2) TheilSenEstimator.estimate(firstHalf, evaluationTimestamp = evalTime) else null
        val secondEstimate = if (secondHalf.size >= 2) TheilSenEstimator.estimate(secondHalf, evaluationTimestamp = evalTime) else null

        val firstSlopeWeekly = firstEstimate?.weeklyChangeKg
        val secondSlopeWeekly = secondEstimate?.weeklyChangeKg
        val fullSlopeWeekly = estimate.weeklyChangeKg

        var slopeDiffWeekly: Double? = null
        var isReversal = false

        if (firstSlopeWeekly != null && secondSlopeWeekly != null) {
            slopeDiffWeekly = abs(firstSlopeWeekly - secondSlopeWeekly)

            val oppositeSigns = (firstSlopeWeekly > 0 && secondSlopeWeekly < 0) || (firstSlopeWeekly < 0 && secondSlopeWeekly > 0)
            val significantMagnitude = abs(firstSlopeWeekly) >= Thresholds.MIN_WEEKLY_CHANGE_FOR_REVERSAL &&
                    abs(secondSlopeWeekly) >= Thresholds.MIN_WEEKLY_CHANGE_FOR_REVERSAL
            isReversal = oppositeSigns && significantMagnitude
        }

        val stdDev = estimate.residualStdDevKg

        val stability = when {
            isReversal -> TrendStability.UNSTABLE
            slopeDiffWeekly != null && slopeDiffWeekly > Thresholds.SLOPE_DIFF_UNSTABLE_KG_PER_WEEK -> TrendStability.UNSTABLE
            stdDev > Thresholds.RESIDUAL_STDDEV_UNSTABLE_KG -> TrendStability.UNSTABLE

            slopeDiffWeekly != null && slopeDiffWeekly > Thresholds.SLOPE_DIFF_MODERATE_KG_PER_WEEK -> TrendStability.MODERATE
            stdDev > Thresholds.RESIDUAL_STDDEV_MODERATE_KG -> TrendStability.MODERATE

            else -> TrendStability.STABLE
        }

        val statusMsg = when {
            isReversal -> "Trend direction reversal detected between subwindows."
            slopeDiffWeekly != null && slopeDiffWeekly > Thresholds.SLOPE_DIFF_UNSTABLE_KG_PER_WEEK -> "Subwindow slope difference is high (${WeightConverter.roundTo(slopeDiffWeekly, 2)} kg/week)."
            stdDev > Thresholds.RESIDUAL_STDDEV_UNSTABLE_KG -> "High residual scatter around trend line (${WeightConverter.roundTo(stdDev, 2)} kg)."
            else -> "Trend is consistent across window."
        }

        val diagnostics = TrendStabilityDiagnostics(
            residualStdDevKg = stdDev,
            firstHalfSlopeKgPerWeek = firstSlopeWeekly,
            secondHalfSlopeKgPerWeek = secondSlopeWeekly,
            fullWindowSlopeKgPerWeek = fullSlopeWeekly,
            slopeDifferenceKgPerWeek = slopeDiffWeekly,
            isDirectionReversal = isReversal,
            stabilityStatusMessage = statusMsg
        )

        return Pair(stability, diagnostics)
    }
}
