package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
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

class ForecastingEngineTest {

    private val dayMs = 86_400_000L
    private val baseTs = 100 * dayMs

    private fun createMeasurements(count: Int, baseKg: Double = 80.0, slopePerDay: Double = -0.05): List<PreparedMeasurement> {
        return (0 until count).map { day ->
            PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = baseKg + (slopePerDay * day),
                elapsedDays = day.toDouble()
            )
        }
    }

    @Test
    fun `generates valid 7, 30, and 90 day forecasts for high confidence stable dataset`() {
        val measurements = createMeasurements(count = 14)
        val evalTs = baseTs + (13 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.35,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 14,
            timeSpanDays = 13.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.1
        )

        val result = ForecastingEngine.generateForecasts(
            preparedMeasurements = measurements,
            estimate = estimate,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            evaluationTimestamp = evalTs,
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        assertTrue(result.overallAvailability.isAvailable)

        val f7 = result.forecasts[ForecastHorizon.DAYS_7]
        val f30 = result.forecasts[ForecastHorizon.DAYS_30]
        val f90 = result.forecasts[ForecastHorizon.DAYS_90]

        assertNotNull(f7)
        assertNotNull(f30)
        assertNotNull(f90)

        assertTrue(f7!!.isAvailable)
        assertTrue(f30!!.isAvailable)
        assertTrue(f90!!.isAvailable)

        assertEquals(79.0, f7.predictedWeightKg!!, 0.05)
        assertEquals(77.85, f30.predictedWeightKg!!, 0.05)
        assertEquals(74.85, f90.predictedWeightKg!!, 0.05)
    }

    @Test
    fun `suppresses 90-day forecast for moderate confidence dataset`() {
        val measurements = createMeasurements(count = 6)
        val evalTs = baseTs + (5 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.75,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 6,
            timeSpanDays = 5.0,
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.1
        )

        val result = ForecastingEngine.generateForecasts(
            preparedMeasurements = measurements,
            estimate = estimate,
            dataSufficiency = DataSufficiency.MODERATE_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            evaluationTimestamp = evalTs,
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val f7 = result.forecasts[ForecastHorizon.DAYS_7]
        val f30 = result.forecasts[ForecastHorizon.DAYS_30]
        val f90 = result.forecasts[ForecastHorizon.DAYS_90]

        assertTrue(f7!!.isAvailable)
        assertTrue(f30!!.isAvailable)
        assertFalse(f90!!.isAvailable)
        assertNull(f90.predictedWeightKg)
    }

    @Test
    fun `rejects negative predicted weight without silent zero clamping`() {
        val measurements = createMeasurements(count = 10, baseKg = 5.0, slopePerDay = -0.5)
        val evalTs = baseTs + (9 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 0.5,
            slopeKgPerDay = -0.5,
            interceptKg = 5.0,
            measurementCount = 10,
            timeSpanDays = 9.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.1
        )

        val result = ForecastingEngine.generateForecasts(
            preparedMeasurements = measurements,
            estimate = estimate,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            evaluationTimestamp = evalTs,
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val f90 = result.forecasts[ForecastHorizon.DAYS_90]
        assertNotNull(f90)
        assertFalse(f90!!.isAvailable)
        assertNull(f90.predictedWeightKg)
    }
}
