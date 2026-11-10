package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailabilityStatus
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAchievementEstimatorTest {

    private val availableStatus = ForecastAvailability(
        status = ForecastAvailabilityStatus.AVAILABLE,
        reason = "Available"
    )

    private val weightLossEstimate = WeightTrendEstimate(
        currentTrendKg = 80.0,
        slopeKgPerDay = -0.1, // -0.7 kg/week
        interceptKg = 80.0,
        measurementCount = 10,
        timeSpanDays = 14.0,
        dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
        trendStability = TrendStability.STABLE,
        residualStdDevKg = 0.2
    )

    private val weightGainEstimate = WeightTrendEstimate(
        currentTrendKg = 60.0,
        slopeKgPerDay = 0.05, // +0.35 kg/week
        interceptKg = 60.0,
        measurementCount = 10,
        timeSpanDays = 14.0,
        dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
        trendStability = TrendStability.STABLE,
        residualStdDevKg = 0.2
    )

    @Test
    fun `target already achieved returns zero days to target`() {
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 80.0,
            estimate = weightLossEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertTrue(result.isAchievable)
        assertTrue(result.isAlreadyAchieved)
        assertEquals(0.0, result.estimatedDaysToTarget!!, 0.001)
    }

    @Test
    fun `target in direction of weight loss estimates correct days to target`() {
        // Target = 75 kg, current = 80 kg, slope = -0.1 kg/day -> 50 days to target
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 75.0,
            estimate = weightLossEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertTrue(result.isAchievable)
        assertFalse(result.isAlreadyAchieved)
        assertEquals(50.0, result.estimatedDaysToTarget!!, 0.01)
        assertNotNull(result.estimatedAchievementTimestamp)
    }

    @Test
    fun `target in direction of weight gain estimates correct days to target`() {
        // Target = 65 kg, current = 60 kg, slope = 0.05 kg/day -> 100 days to target
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 65.0,
            estimate = weightGainEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertTrue(result.isAchievable)
        assertFalse(result.isAlreadyAchieved)
        assertEquals(100.0, result.estimatedDaysToTarget!!, 0.01)
    }

    @Test
    fun `target opposite direction of travel returns unachievable`() {
        // Current = 80 kg, target = 85 kg, but slope = -0.1 (losing weight)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 85.0,
            estimate = weightLossEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
    }

    @Test
    fun `zero slope returns unachievable`() {
        val zeroSlopeEstimate = weightLossEstimate.copy(slopeKgPerDay = 0.0)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 70.0,
            estimate = zeroSlopeEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
    }

    @Test
    fun `target date exceeding 10 years horizon returns unachievable`() {
        // Slope = -0.0001 kg/day, target diff = 10 kg -> 100,000 days (> 3650 days max horizon)
        val slowEstimate = weightLossEstimate.copy(slopeKgPerDay = -0.0001)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 70.0,
            estimate = slowEstimate,
            evaluationTimestamp = 1000L,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
    }
}
