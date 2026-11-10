package com.johnpaulpatigas.weightcast.domain.trend.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WeightTrendEstimateTest {

    @Test
    fun `valid construction calculates derived weekly change correctly for zero slope`() {
        val estimate = WeightTrendEstimate(
            currentTrendKg = 75.0,
            slopeKgPerDay = 0.0,
            interceptKg = 75.0,
            measurementCount = 10,
            timeSpanDays = 14.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.2
        )

        assertEquals(0.0, estimate.weeklyChangeKg, 0.0001)
        assertEquals(75.0, estimate.currentTrendKg, 0.0001)
    }

    @Test
    fun `valid construction calculates derived weekly change correctly for negative slope`() {
        val estimate = WeightTrendEstimate(
            currentTrendKg = 70.0,
            slopeKgPerDay = -0.1,
            interceptKg = 71.4,
            measurementCount = 14,
            timeSpanDays = 14.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.15
        )

        assertEquals(-0.7, estimate.weeklyChangeKg, 0.0001)
    }

    @Test
    fun `valid construction calculates derived weekly change correctly for positive slope`() {
        val estimate = WeightTrendEstimate(
            currentTrendKg = 82.5,
            slopeKgPerDay = 0.05,
            interceptKg = 80.0,
            measurementCount = 7,
            timeSpanDays = 10.0,
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.MODERATE,
            residualStdDevKg = 0.4
        )

        assertEquals(0.35, estimate.weeklyChangeKg, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `construction with NaN currentTrendKg throws IllegalArgumentException`() {
        WeightTrendEstimate(
            currentTrendKg = Double.NaN,
            slopeKgPerDay = -0.1,
            interceptKg = 70.0,
            measurementCount = 5,
            timeSpanDays = 7.0,
            dataSufficiency = DataSufficiency.LOW_CONFIDENCE,
            trendStability = TrendStability.MODERATE,
            residualStdDevKg = 0.5
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `construction with negative currentTrendKg throws IllegalArgumentException`() {
        WeightTrendEstimate(
            currentTrendKg = -5.0,
            slopeKgPerDay = 0.0,
            interceptKg = 0.0,
            measurementCount = 5,
            timeSpanDays = 7.0,
            dataSufficiency = DataSufficiency.LOW_CONFIDENCE,
            trendStability = TrendStability.UNSTABLE,
            residualStdDevKg = 0.1
        )
    }

    @Test
    fun `TrendEstimationResult handles nullable estimate when data is insufficient`() {
        val diagnostics = TrendDiagnostics(
            validMeasurementCount = 1,
            rejectedMeasurementCount = 0,
            timeSpanDays = 0.0,
            coverageRatio = 1.0,
            validPairwiseSlopeCount = 0,
            stabilityDiagnostics = null,
            statusMessage = "Insufficient data points"
        )

        val result = TrendEstimationResult(
            estimate = null,
            sufficiency = DataSufficiency.INSUFFICIENT_DATA,
            diagnostics = diagnostics
        )

        assertNull(result.estimate)
        assertFalse(result.isAvailable)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, result.sufficiency)
    }

    @Test
    fun `DataSufficiency and TrendStability can vary independently`() {
        val estimate = WeightTrendEstimate(
            currentTrendKg = 75.0,
            slopeKgPerDay = 0.01,
            interceptKg = 74.8,
            measurementCount = 30,
            timeSpanDays = 30.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.UNSTABLE,
            residualStdDevKg = 1.8
        )

        assertEquals(DataSufficiency.HIGH_CONFIDENCE, estimate.dataSufficiency)
        assertEquals(TrendStability.UNSTABLE, estimate.trendStability)
    }
}
