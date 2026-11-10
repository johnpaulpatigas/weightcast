package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.TargetAchievementEstimate
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement
import com.johnpaulpatigas.weightcast.domain.trend.engine.TheilSenEstimator
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object TargetAchievementEstimator {

    /** Maximum allowed forecast horizon for target achievement estimation (10 years / 3,650 days). */
    private const val MAX_HORIZON_DAYS = 3650.0

    /** Minimum required ratio of valid reaching bootstrap trajectories (70%). */
    private const val MIN_REACHING_RATIO_THRESHOLD = 0.70

    /**
     * Estimates target weight achievement date and uncertainty range from individual bootstrap crossing trajectories.
     *
     * @param targetWeightKg Target goal weight in kilograms.
     * @param preparedMeasurements Prepared daily measurement representatives sorted by timestamp.
     * @param estimate Current statistical trend estimate.
     * @param evaluationTimestamp Current evaluation timestamp in epoch millis.
     * @param overallAvailability Evaluated forecast availability classification.
     * @param bootstrapIterations Number of iterations for target crossing uncertainty calculation (default 300).
     * @param randomSeed Optional random seed for reproducible test executions.
     * @return [TargetAchievementEstimate] detailing estimated date, crossing day bounds, reaching ratio, and availability status.
     */
    fun estimateTargetAchievement(
        targetWeightKg: Double?,
        preparedMeasurements: List<PreparedMeasurement>,
        estimate: WeightTrendEstimate?,
        evaluationTimestamp: Long = System.currentTimeMillis(),
        overallAvailability: ForecastAvailability,
        bootstrapIterations: Int = 300,
        randomSeed: Long? = null
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
                reachingRatio = null,
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
                reachingRatio = null,
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
                reachingRatio = 1.0,
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
                reachingRatio = 0.0,
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
                reachingRatio = 0.0,
                availabilityReason = "Current weight trend is moving away from the target goal."
            )
        }

        val pointDaysToTarget = diff / slopePerDay

        if (pointDaysToTarget > MAX_HORIZON_DAYS) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                reachingRatio = 0.0,
                availabilityReason = "Estimated target achievement exceeds maximum 10-year horizon."
            )
        }

        // --- Bootstrap Crossing Trajectory Analysis ---
        val sorted = preparedMeasurements.sortedBy { it.timestamp }
        val n = sorted.size

        if (n < 2 || bootstrapIterations < 10) {
            val targetTimestamp = evaluationTimestamp + (pointDaysToTarget * DataPreparation.MILLIS_PER_DAY).toLong()
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = targetTimestamp,
                estimatedDaysToTarget = pointDaysToTarget,
                lowerBoundDaysToTarget = max(0.0, pointDaysToTarget * 0.85),
                upperBoundDaysToTarget = pointDaysToTarget * 1.15,
                isAchievable = true,
                isAlreadyAchieved = false,
                reachingRatio = 1.0,
                availabilityReason = "Target achievement estimated based on current statistical trend slope."
            )
        }

        val baselineTs = sorted.first().timestamp
        val evalDays = (evaluationTimestamp - baselineTs) / DataPreparation.MILLIS_PER_DAY

        val residuals = DoubleArray(n)
        var sumResiduals = 0.0
        for (i in 0 until n) {
            val fitted = estimate.interceptKg + (estimate.slopeKgPerDay * sorted[i].elapsedDays)
            residuals[i] = sorted[i].weightKg - fitted
            sumResiduals += residuals[i]
        }
        val meanResidual = sumResiduals / n
        val centeredResiduals = DoubleArray(n) { i -> residuals[i] - meanResidual }

        val random = randomSeed?.let { Random(it) } ?: Random.Default

        val maxPairs = n * (n - 1) / 2
        val slopesBuffer = DoubleArray(maxPairs)
        val interceptsBuffer = DoubleArray(n)
        val syntheticWeights = DoubleArray(n)
        val validCrossingDays = mutableListOf<Double>()

        for (b in 0 until bootstrapIterations) {
            for (i in 0 until n) {
                val resIdx = random.nextInt(n)
                val eStar = centeredResiduals[resIdx]
                val fitted = estimate.interceptKg + (estimate.slopeKgPerDay * sorted[i].elapsedDays)
                syntheticWeights[i] = fitted + eStar
            }

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

            val beta1Star = if (slopeCount > 0) TheilSenEstimator.calculateMedian(slopesBuffer, slopeCount) else estimate.slopeKgPerDay

            for (i in 0 until n) {
                interceptsBuffer[i] = syntheticWeights[i] - (beta1Star * sorted[i].elapsedDays)
            }
            val beta0Star = TheilSenEstimator.calculateMedian(interceptsBuffer, n)

            val currentTrendStar = beta0Star + (beta1Star * evalDays)
            val diffStar = targetWeightKg - currentTrendStar

            if (abs(beta1Star) > 1e-6 && (diffStar / beta1Star) > 0.0) {
                val daysStar = diffStar / beta1Star
                if (daysStar > 0.0 && daysStar <= MAX_HORIZON_DAYS) {
                    validCrossingDays.add(daysStar)
                }
            }
        }

        val reachingRatio = validCrossingDays.size.toDouble() / bootstrapIterations.toDouble()

        if (reachingRatio < MIN_REACHING_RATIO_THRESHOLD) {
            return TargetAchievementEstimate(
                targetWeightKg = targetWeightKg,
                currentTrendWeightKg = currentTrendKg,
                estimatedAchievementTimestamp = null,
                estimatedDaysToTarget = null,
                lowerBoundDaysToTarget = null,
                upperBoundDaysToTarget = null,
                isAchievable = false,
                isAlreadyAchieved = false,
                reachingRatio = reachingRatio,
                availabilityReason = "High slope uncertainty: only ${(reachingRatio * 100).toInt()}% of bootstrap trajectories reach target goal."
            )
        }

        validCrossingDays.sort()
        val lowerIdx = max(0, min(validCrossingDays.size - 1, (validCrossingDays.size * 0.025).toInt()))
        val upperIdx = max(0, min(validCrossingDays.size - 1, (validCrossingDays.size * 0.975).toInt()))

        var lowerBoundDays = validCrossingDays[lowerIdx]
        var upperBoundDays = validCrossingDays[upperIdx]

        if (lowerBoundDays > pointDaysToTarget) lowerBoundDays = pointDaysToTarget
        if (upperBoundDays < pointDaysToTarget) upperBoundDays = pointDaysToTarget

        val targetTimestamp = evaluationTimestamp + (pointDaysToTarget * DataPreparation.MILLIS_PER_DAY).toLong()

        return TargetAchievementEstimate(
            targetWeightKg = targetWeightKg,
            currentTrendWeightKg = currentTrendKg,
            estimatedAchievementTimestamp = targetTimestamp,
            estimatedDaysToTarget = pointDaysToTarget,
            lowerBoundDaysToTarget = lowerBoundDays,
            upperBoundDaysToTarget = upperBoundDays,
            isAchievable = true,
            isAlreadyAchieved = false,
            reachingRatio = reachingRatio,
            availabilityReason = "Target achievement estimated from bootstrap crossing trajectories."
        )
    }
}
