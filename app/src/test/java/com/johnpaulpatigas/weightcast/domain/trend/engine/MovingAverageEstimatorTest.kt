package com.johnpaulpatigas.weightcast.domain.trend.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovingAverageEstimatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `calculateMovingAverageAt with no measurements in window returns null`() {
        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0)
        )

        // Evaluation time 20 days later -> window is [113d, 120d], baseTime is 100d -> 0 measurements
        val result = MovingAverageEstimator.calculateMovingAverageAt(measurements, baseTime + (20 * dayMs))
        assertNull(result)
    }

    @Test
    fun `calculateMovingAverageAt uses only trailing 7 days and excludes future measurements`() {
        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 80.0), // -10d from eval
            PreparedMeasurement(timestamp = baseTime + (5 * dayMs), elapsedDays = 5.0, weightKg = 78.0), // -5d
            PreparedMeasurement(timestamp = baseTime + (8 * dayMs), elapsedDays = 8.0, weightKg = 76.0), // -2d
            PreparedMeasurement(timestamp = baseTime + (10 * dayMs), elapsedDays = 10.0, weightKg = 74.0), // Evaluation time
            PreparedMeasurement(timestamp = baseTime + (12 * dayMs), elapsedDays = 12.0, weightKg = 70.0)  // Future -> MUST BE EXCLUDED
        )

        val evalTime = baseTime + (10 * dayMs)
        val windowStart = evalTime - (7 * dayMs) // baseTime + 3d

        // Window [baseTime+3d, baseTime+10d] contains points at 5d (78.0), 8d (76.0), 10d (74.0)
        // Average = (78.0 + 76.0 + 74.0) / 3 = 76.0
        val ma = MovingAverageEstimator.calculateMovingAverageAt(measurements, evalTime)
        assertEquals(76.0, ma!!, 0.001)
    }

    @Test
    fun `calculateMovingAverageSeries computes series for all prepared measurements`() {
        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 80.0),
            PreparedMeasurement(timestamp = baseTime + (1 * dayMs), elapsedDays = 1.0, weightKg = 78.0),
            PreparedMeasurement(timestamp = baseTime + (2 * dayMs), elapsedDays = 2.0, weightKg = 76.0)
        )

        val series = MovingAverageEstimator.calculateMovingAverageSeries(measurements)
        assertEquals(3, series.size)

        // Point 1: avg(80.0) = 80.0
        assertEquals(80.0, series[0].second, 0.001)
        // Point 2: avg(80.0, 78.0) = 79.0
        assertEquals(79.0, series[1].second, 0.001)
        // Point 3: avg(80.0, 78.0, 76.0) = 78.0
        assertEquals(78.0, series[2].second, 0.001)
    }
}
