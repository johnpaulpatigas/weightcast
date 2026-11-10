package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastAvailabilityStatus
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastEligibilityEvaluatorTest {

    private val validEstimate = WeightTrendEstimate(
        currentTrendKg = 75.0,
        slopeKgPerDay = -0.05,
        interceptKg = 75.0,
        measurementCount = 10,
        timeSpanDays = 14.0,
        dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
        trendStability = TrendStability.STABLE,
        residualStdDevKg = 0.2
    )

    @Test
    fun `insufficient data suppresses all forecast horizons`() {
        ForecastHorizon.entries.forEach { horizon ->
            val result = ForecastEligibilityEvaluator.evaluate(
                dataSufficiency = DataSufficiency.INSUFFICIENT_DATA,
                trendStability = TrendStability.STABLE,
                estimate = validEstimate,
                horizon = horizon
            )
            assertFalse(result.isAvailable)
            assertEquals(ForecastAvailabilityStatus.INSUFFICIENT_DATA, result.status)
        }
    }

    @Test
    fun `low confidence suppresses all forecast horizons`() {
        ForecastHorizon.entries.forEach { horizon ->
            val result = ForecastEligibilityEvaluator.evaluate(
                dataSufficiency = DataSufficiency.LOW_CONFIDENCE,
                trendStability = TrendStability.STABLE,
                estimate = validEstimate,
                horizon = horizon
            )
            assertFalse(result.isAvailable)
            assertEquals(ForecastAvailabilityStatus.LOW_CONFIDENCE, result.status)
        }
    }

    @Test
    fun `moderate confidence stable trend allows 7-day and 30-day, suppresses 90-day`() {
        val avail7 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_7
        )
        assertTrue(avail7.isAvailable)

        val avail30 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_30
        )
        assertTrue(avail30.isAvailable)

        val avail90 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_90
        )
        assertFalse(avail90.isAvailable)
        assertEquals(ForecastAvailabilityStatus.LOW_CONFIDENCE, avail90.status)
    }

    @Test
    fun `moderate confidence moderate stability allows 7-day only`() {
        val avail7 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.MODERATE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_7
        )
        assertTrue(avail7.isAvailable)

        val avail30 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.MODERATE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_30
        )
        assertFalse(avail30.isAvailable)

        val avail90 = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.MODERATE,
            estimate = validEstimate,
            horizon = ForecastHorizon.DAYS_90
        )
        assertFalse(avail90.isAvailable)
    }

    @Test
    fun `high confidence stable trend allows all horizons`() {
        ForecastHorizon.entries.forEach { horizon ->
            val result = ForecastEligibilityEvaluator.evaluate(
                dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
                trendStability = TrendStability.STABLE,
                estimate = validEstimate,
                horizon = horizon
            )
            assertTrue(result.isAvailable)
            assertEquals(ForecastAvailabilityStatus.AVAILABLE, result.status)
        }
    }

    @Test
    fun `unstable trend suppresses forecasts across all sufficiency levels`() {
        ForecastHorizon.entries.forEach { horizon ->
            val result = ForecastEligibilityEvaluator.evaluate(
                dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
                trendStability = TrendStability.UNSTABLE,
                estimate = validEstimate,
                horizon = horizon
            )
            assertFalse(result.isAvailable)
            assertEquals(ForecastAvailabilityStatus.UNSTABLE_TREND, result.status)
        }
    }

    @Test
    fun `null estimate suppresses forecasts`() {
        val nullResult = ForecastEligibilityEvaluator.evaluate(
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            estimate = null,
            horizon = ForecastHorizon.DAYS_7
        )
        assertFalse(nullResult.isAvailable)
        assertEquals(ForecastAvailabilityStatus.INSUFFICIENT_DATA, nullResult.status)
    }
}
