package com.johnpaulpatigas.weightcast.domain.trend.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovingAverageEstimatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `calculateMovingAverageAt with no measurements in window returns null`() {
        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0)
        )

        val result = MovingAverageEstimator.calculateMovingAverageAt(measurements, baseTime + (20 * dayMs))
        assertNull(result)
    }

    @Test
    fun `calculateMovingAverageAt uses exactly 7 UTC calendar dates and excludes future measurements`() {
        // Evaluation on day 100 + 10d = day 110.
        // 7 UTC calendar dates: day 104, 105, 106, 107, 108, 109, 110.
        val evalTime = baseTime + (10 * dayMs) // Day 110

        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime + (3 * dayMs), elapsedDays = 3.0, weightKg = 90.0),  // Day 103 -> EXCLUDED (8th day back)
            PreparedMeasurement(timestamp = baseTime + (4 * dayMs), elapsedDays = 4.0, weightKg = 80.0),  // Day 104 -> INCLUDED (7th day back boundary)
            PreparedMeasurement(timestamp = baseTime + (7 * dayMs), elapsedDays = 7.0, weightKg = 76.0),  // Day 107 -> INCLUDED
            PreparedMeasurement(timestamp = evalTime, elapsedDays = 10.0, weightKg = 72.0),               // Day 110 -> INCLUDED (eval timestamp)
            PreparedMeasurement(timestamp = evalTime + 1000L, elapsedDays = 10.0, weightKg = 60.0)        // Future -> EXCLUDED
        )

        val ma = MovingAverageEstimator.calculateMovingAverageAt(measurements, evalTime)
        // Average of included points (80.0 + 76.0 + 72.0) / 3 = 76.0
        assertEquals(76.0, ma!!, 0.001)
    }

    @Test
    fun `calculateMovingAverageAt boundary tests`() {
        val evalDay = 20L
        val evalTime = evalDay * dayMs // UTC midnight of Day 20

        val pointDay13 = PreparedMeasurement(timestamp = 13 * dayMs, elapsedDays = 13.0, weightKg = 100.0) // Day 13 -> Excluded (8 dates back)
        val pointDay14 = PreparedMeasurement(timestamp = 14 * dayMs, elapsedDays = 14.0, weightKg = 80.0)  // Day 14 -> Included (Boundary: evalDay - 6)
        val pointDay20 = PreparedMeasurement(timestamp = evalTime, elapsedDays = 20.0, weightKg = 70.0)     // Day 20 -> Included

        val measurements = listOf(pointDay13, pointDay14, pointDay20)

        val ma = MovingAverageEstimator.calculateMovingAverageAt(measurements, evalTime)
        // Day 14 and Day 20 included: avg(80.0, 70.0) = 75.0
        assertEquals(75.0, ma!!, 0.001)
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

        assertEquals(80.0, series[0].second, 0.001)
        assertEquals(79.0, series[1].second, 0.001)
        assertEquals(78.0, series[2].second, 0.001)
    }
}
