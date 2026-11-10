package com.johnpaulpatigas.weightcast.domain.trend.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TheilSenEstimatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `1 perfect constant weight produces zero slope and exact trend weight`() {
        val measurements = (0..10).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 75.0)
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertNotNull(estimate)
        assertEquals(0.0, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(0.0, estimate.weeklyChangeKg, 0.0001)
        assertEquals(75.0, estimate.currentTrendKg, 0.0001)
        assertEquals(75.0, estimate.interceptKg, 0.0001)
        assertEquals(0.0, estimate.residualStdDevKg, 0.0001)
    }

    @Test
    fun `2 perfect linear weight loss calculates exact slope`() {
        // -0.1 kg per day = -0.7 kg per week
        val measurements = (0..10).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 80.0 - (0.1 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertNotNull(estimate)
        assertEquals(-0.1, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(-0.7, estimate.weeklyChangeKg, 0.0001)
        assertEquals(79.0, estimate.currentTrendKg, 0.0001) // 80.0 - 1.0 = 79.0 at day 10
        assertEquals(80.0, estimate.interceptKg, 0.0001)
    }

    @Test
    fun `3 perfect linear weight gain calculates exact slope`() {
        // +0.05 kg per day = +0.35 kg per week
        val measurements = (0..10).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0 + (0.05 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertNotNull(estimate)
        assertEquals(0.05, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(0.35, estimate.weeklyChangeKg, 0.0001)
        assertEquals(70.5, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `4 zero slope`() {
        val measurements = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 68.0),
            PreparedMeasurement(timestamp = baseTime + (5 * dayMs), elapsedDays = 5.0, weightKg = 68.0),
            PreparedMeasurement(timestamp = baseTime + (10 * dayMs), elapsedDays = 10.0, weightKg = 68.0)
        )

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertEquals(0.0, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(68.0, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `5 fractional daily slopes`() {
        // -0.5 kg / 7 days = -0.07142857 kg/day
        val targetWeeklyLoss = -0.5
        val dailySlope = targetWeeklyLoss / 7.0

        val measurements = (0..14).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 90.0 + (dailySlope * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (14 * dayMs))
        assertEquals(targetWeeklyLoss, estimate!!.weeklyChangeKg, 0.0001)
    }

    @Test
    fun `6 irregular measurement intervals`() {
        val days = listOf(0.0, 2.0, 7.0, 15.0, 30.0)
        val measurements = days.map { day ->
            PreparedMeasurement(timestamp = baseTime + (day.toLong() * dayMs), elapsedDays = day, weightKg = 85.0 - (0.1 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (30 * dayMs))
        assertEquals(-0.1, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(82.0, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `7 missing calendar days`() {
        // Gap from day 2 to day 10
        val days = listOf(0.0, 1.0, 2.0, 10.0, 11.0, 12.0)
        val measurements = days.map { day ->
            PreparedMeasurement(timestamp = baseTime + (day.toLong() * dayMs), elapsedDays = day, weightKg = 70.0 + (0.2 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (12 * dayMs))
        assertEquals(0.2, estimate!!.slopeKgPerDay, 0.0001)
        assertEquals(72.4, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `8 one extreme high outlier does not distort slope`() {
        val measurements = mutableListOf<PreparedMeasurement>()
        for (day in 0..10) {
            val weight = if (day == 5) 95.0 else 80.0 - (0.1 * day) // Day 5 is +15kg scale spike
            measurements.add(PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = weight))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertNotNull(estimate)
        // Median pairwise slope remains exact -0.1 kg/day despite 15kg spike
        assertEquals(-0.1, estimate!!.slopeKgPerDay, 0.001)
    }

    @Test
    fun `9 one extreme low outlier does not distort slope`() {
        val measurements = mutableListOf<PreparedMeasurement>()
        for (day in 0..10) {
            val weight = if (day == 3) 50.0 else 75.0 + (0.1 * day) // Day 3 is -25kg spike
            measurements.add(PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = weight))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertNotNull(estimate)
        assertEquals(0.1, estimate!!.slopeKgPerDay, 0.001)
    }

    @Test
    fun `10 multiple outliers remain robust`() {
        // 2 spikes out of 9 measurements
        val measurements = (0..8).map { day ->
            val weight = when (day) {
                2 -> 100.0
                6 -> 40.0
                else -> 80.0 - (0.2 * day)
            }
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = weight)
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (8 * dayMs))
        assertNotNull(estimate)
        assertEquals(-0.2, estimate!!.slopeKgPerDay, 0.05)
    }

    @Test
    fun `11 duplicate timestamps aggregated via preparation behave cleanly`() {
        val raw = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0),
            PreparedMeasurement(timestamp = baseTime + dayMs, elapsedDays = 1.0, weightKg = 70.5)
        )

        val estimate = TheilSenEstimator.estimate(raw, evaluationTimestamp = baseTime + dayMs)
        assertNotNull(estimate)
        assertEquals(0.5, estimate!!.slopeKgPerDay, 0.0001)
    }

    @Test
    fun `12 insufficient measurements returns null`() {
        val singlePoint = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0)
        )
        assertNull(TheilSenEstimator.estimate(singlePoint))
    }

    @Test
    fun `13 empty input returns null`() {
        assertNull(TheilSenEstimator.estimate(emptyList()))
    }

    @Test
    fun `14 non-finite calculations handling`() {
        val array = doubleArrayOf(1.0, 3.0, 2.0, 5.0)
        val median = TheilSenEstimator.calculateMedian(array, 4)
        assertEquals(2.5, median, 0.0001)
    }

    @Test
    fun `15 correct intercept at reference timestamp`() {
        val measurements = (0..5).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 82.0 - (0.3 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime)
        assertEquals(82.0, estimate!!.interceptKg, 0.0001)
        assertEquals(82.0, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `16 correct current trend weight at evaluation time`() {
        val measurements = (0..10).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 100.0 - (0.5 * day))
        }

        // Evaluate at day 10
        val estimate10 = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        assertEquals(95.0, estimate10!!.currentTrendKg, 0.0001)

        // Evaluate at day 15 (future eval time relative to dataset)
        val estimate15 = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (15 * dayMs))
        assertEquals(92.5, estimate15!!.currentTrendKg, 0.0001)
    }

    @Test
    fun `17 correct weekly change`() {
        val measurements = (0..7).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0 + (0.1 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements)
        assertEquals(0.7, estimate!!.weeklyChangeKg, 0.0001)
    }

    @Test
    fun `18 correct even-sized median`() {
        val evenArray = doubleArrayOf(10.0, 20.0, 30.0, 40.0)
        val median = TheilSenEstimator.calculateMedian(evenArray, 4)
        assertEquals(25.0, median, 0.0001)

        val oddArray = doubleArrayOf(10.0, 20.0, 30.0)
        val medianOdd = TheilSenEstimator.calculateMedian(oddArray, 3)
        assertEquals(20.0, medianOdd, 0.0001)
    }

    @Test
    fun `19 residual standard deviation calculation`() {
        val measurements = (0..4).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0)
        }

        val estimate = TheilSenEstimator.estimate(measurements)
        assertEquals(0.0, estimate!!.residualStdDevKg, 0.0001)
    }

    @Test
    fun `20 large dataset with 500 measurements runs fast and accurately`() {
        val measurements = (0 until 500).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 95.0 - (0.05 * day))
        }

        val startTime = System.currentTimeMillis()
        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (499 * dayMs))
        val durationMs = System.currentTimeMillis() - startTime

        assertNotNull(estimate)
        assertEquals(-0.05, estimate!!.slopeKgPerDay, 0.001)
        assertEquals(70.05, estimate.currentTrendKg, 0.001)
        assertTrue("Execution took $durationMs ms, expected < 1000 ms", durationMs < 1000)
    }
}
