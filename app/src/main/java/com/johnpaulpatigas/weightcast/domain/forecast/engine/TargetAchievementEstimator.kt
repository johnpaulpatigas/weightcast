package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.TargetAchievementEstimate
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import kotlin.math.abs
import kotlin.math.max

object TargetAchievementEstimator {

    /** Maximum allowed forecast horizon for target achievement estimation (10 years / 3,650 days). */
    private const val MAX_HORIZON_DAYS = 3650.0

    /**
     * Estimates target weight achievement date and uncertainty range based on the fitted Theil-Sen slope.
     *
     * @param targetWeightKg Target goal weight in kilograms.
     * @param estimate Current statistical trend estimate.
     * @param evaluationTimestamp Current evaluation timestamp in epoch millis.
     * @param overallAvailability Evaluated forecast availability classification.
     * @return [TargetAchievementEstimate] detailing estimated date, days to target, and availability status.
     */
    fun estimateTargetAchievement(
        targetWeightKg: Double?,
        estimate: WeightTrendEstimate?,
        evaluationTimestamp: Long = System.currentTimeMillis(),
        overallAvailability: ForecastAvailability
    ): TargetAchievementEstimate {
        if (targetWeightKg == null || targetWeightKg <= 0.0) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg ?: 0.0,
                currentTrendWeightKg = estimate?.currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                availabilityReason = "No target weight goal specified."
            )
        }

        if (estimate == null || !overallAvailability.isAvailable) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = estimate?.currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                availabilityReason = overallAvailability.reason
            )
        }

        val currentTrendKg = estimate.currentTrendKg
        val slopePerDay = estimate.slopeKgPerDay

        val diff = targetWeightKg - currentTrendKg

        // Check if goal is already achieved (within 0.1 kg or past target)
        val isAlreadyAchieved = abs(diff) <= 0.1 ||
                (diff < 0 && currentTrendKg <= targetWeightKg) ||
                (diff > 0 && currentTrendKg >= targetWeightKg)

        if (isAlreadyAchieved) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = evaluationTimestamp,
                estimatedDaysToTarget = 0.0,
                lowerBoundDaysToTarget = 0.0,
                upperBoundDaysToTarget = 0.0,
                isAchievable = true,
                isAlreadyAchieved = true,
                availabilityReason = "Target weight goal has already been achieved."
            )
        }

        if (slopePerDay == 0.0) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                availabilityReason = "Current weight trend is stable with zero daily slope."
            )
        }

        val isMovingTowardsGoal = (diff / slopePerDay) > 0.0

        if (!isMovingTowardsGoal) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                availabilityReason = "Current weight trend is moving away from the target goal."
            )
        }

        val daysToTarget = diff / slopePerDay

        if (daysToTarget > MAX_HORIZON_DAYS) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                availabilityReason = "Estimated target achievement exceeds maximum 10-year horizon."
            )
        }

        val lowerBoundDays = max(0.0, daysToTarget * 0.85)
        val upperBoundDays = daysToTarget * 1.15

        val targetTimestamp = evaluationTimestamp + (daysToTarget * DataPreparation.MILLIS_PER_DAY).toLong()

        return TargetAchievementEstimate(
            targetWeightKg = targetWeightKg,
            currentTrendWeightKg = currentTrendKg,
            estimatedAchievementTimestamp = targetTimestamp,
            estimatedDaysToTarget = daysToTarget,
            lowerBoundDaysToTarget = lowerBoundDays,
            upperBoundDaysToTarget = upperBoundDays,
            isAchievable = true,
            isAlreadyAchieved = false,
            availabilityReason = "Target achievement estimated based on current statistical trend slope."
        )
    }
}
