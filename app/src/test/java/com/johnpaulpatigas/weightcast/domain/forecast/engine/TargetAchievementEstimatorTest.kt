package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailability
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailabilityStatus
import com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement
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

    private val dayMs = 86_400_000L
    private val baseTs = 1000L

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

    private fun sampleLossMeasurements(): List<PreparedMeasurement> {
        return (0 until 14).map { day ->
            PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = 80.0 - (0.1 * day),
                elapsedDays = day.toDouble()
            )
        }
    }

    private fun sampleGainMeasurements(): List<PreparedMeasurement> {
        return (0 until 14).map { day ->
            PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = 60.0 + (0.05 * day),
                elapsedDays = day.toDouble()
            )
        }
    }

    @Test
    fun `target already achieved returns zero days to target`() {
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 80.0,
            preparedMeasurements = sampleLossMeasurements(),
            estimate = weightLossEstimate,
            evaluationTimestamp = baseTs + (13 * dayMs),
            overallAvailability = availableStatus
        )

        assertTrue(result.isAchievable)
        assertTrue(result.isAlreadyAchieved)
        assertEquals(0.0, result.estimatedDaysToTarget!!, 0.001)
        assertEquals(1.0, result.reachingRatio!!, 0.001)
    }

    @Test
    fun `target in direction of weight loss estimates correct days to target and bootstrap bounds`() {
        val evalTs = baseTs + (13 * dayMs)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 75.0,
            preparedMeasurements = sampleLossMeasurements(),
            estimate = weightLossEstimate,
            evaluationTimestamp = evalTs,
            overallAvailability = availableStatus,
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        assertTrue(result.isAchievable)
        assertFalse(result.isAlreadyAchieved)
        assertEquals(50.0, result.estimatedDaysToTarget!!, 0.1)
        assertNotNull(result.estimatedAchievementTimestamp)
        assertNotNull(result.lowerBoundDaysToTarget)
        assertNotNull(result.upperBoundDaysToTarget)
        assertTrue(result.reachingRatio!! >= 0.70)
        assertTrue(result.lowerBoundDaysToTarget!! <= result.estimatedDaysToTarget!!)
        assertTrue(result.estimatedDaysToTarget!! <= result.upperBoundDaysToTarget!!)
    }

    @Test
    fun `target in direction of weight gain estimates correct days to target`() {
        val evalTs = baseTs + (13 * dayMs)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 65.0,
            preparedMeasurements = sampleGainMeasurements(),
            estimate = weightGainEstimate,
            evaluationTimestamp = evalTs,
            overallAvailability = availableStatus,
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        assertTrue(result.isAchievable)
        assertFalse(result.isAlreadyAchieved)
        assertEquals(100.0, result.estimatedDaysToTarget!!, 0.1)
        assertTrue(result.reachingRatio!! >= 0.70)
    }

    @Test
    fun `target opposite direction of travel returns unachievable`() {
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 85.0,
            preparedMeasurements = sampleLossMeasurements(),
            estimate = weightLossEstimate,
            evaluationTimestamp = baseTs,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
        assertEquals(0.0, result.reachingRatio!!, 0.001)
    }

    @Test
    fun `zero slope returns unachievable`() {
        val zeroSlopeEstimate = weightLossEstimate.copy(slopeKgPerDay = 0.0)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 70.0,
            preparedMeasurements = sampleLossMeasurements(),
            estimate = zeroSlopeEstimate,
            evaluationTimestamp = baseTs,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
        assertEquals(0.0, result.reachingRatio!!, 0.001)
    }

    @Test
    fun `target date exceeding 10 years horizon returns unachievable`() {
        val slowEstimate = weightLossEstimate.copy(slopeKgPerDay = -0.0001)
        val result = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = 70.0,
            preparedMeasurements = sampleLossMeasurements(),
            estimate = slowEstimate,
            evaluationTimestamp = baseTs,
            overallAvailability = availableStatus
        )

        assertFalse(result.isAchievable)
        assertNull(result.estimatedDaysToTarget)
    }
}
