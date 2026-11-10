package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PredictionIntervalEstimatorTest {

    private val dayMs = 86_400_000L
    private val baseTs = 100 * dayMs

    private fun createDataset(count: Int, slopePerDay: Double = -0.05, noiseStd: Double = 0.2, seed: Long = 42L): List<com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement> {
        val rng = Random(seed)
        return (0 until count).map { day ->
            val noise = if (noiseStd > 0.0) rng.nextGaussian() * noiseStd else 0.0
            com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = 80.0 + (slopePerDay * day) + noise,
                elapsedDays = day.toDouble()
            )
        }
    }

    @Test
    fun `deterministic random seed produces identical prediction intervals`() {
        val measurements = createDataset(count = 14, slopePerDay = -0.05, noiseStd = 0.2)
        val evalTs = baseTs + (13 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.35,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 14,
            timeSpanDays = 13.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.2
        )

        val pi1 = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 7.0,
            bootstrapIterations = 200,
            randomSeed = 12345L
        )

        val pi2 = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 7.0,
            bootstrapIterations = 200,
            randomSeed = 12345L
        )

        assertNotNull(pi1)
        assertNotNull(pi2)
        assertEquals(pi1!!.lowerBoundKg, pi2!!.lowerBoundKg, 1e-6)
        assertEquals(pi1.upperBoundKg, pi2.upperBoundKg, 1e-6)
    }

    @Test
    fun `prediction interval satisfies lower bound less than point estimate less than upper bound`() {
        val measurements = createDataset(count = 14, slopePerDay = -0.05, noiseStd = 0.2)
        val evalTs = baseTs + (13 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.35,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 14,
            timeSpanDays = 13.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.2
        )

        val pi = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 30.0,
            bootstrapIterations = 300,
            randomSeed = 42L
        )

        assertNotNull(pi)
        val pointEst = estimate.interceptKg + (estimate.slopeKgPerDay * (13.0 + 30.0))
        assertTrue(pi!!.lowerBoundKg <= pointEst)
        assertTrue(pointEst <= pi.upperBoundKg)
    }

    @Test
    fun `prediction interval widens with longer forecast horizons`() {
        val measurements = createDataset(count = 14, slopePerDay = -0.05, noiseStd = 0.3)
        val evalTs = baseTs + (13 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.35,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 14,
            timeSpanDays = 13.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.3
        )

        val pi7 = PredictionIntervalEstimator.estimate(measurements, estimate, evalTs, 7.0, bootstrapIterations = 300, randomSeed = 42L)
        val pi30 = PredictionIntervalEstimator.estimate(measurements, estimate, evalTs, 30.0, bootstrapIterations = 300, randomSeed = 42L)
        val pi90 = PredictionIntervalEstimator.estimate(measurements, estimate, evalTs, 90.0, bootstrapIterations = 300, randomSeed = 42L)

        assertNotNull(pi7)
        assertNotNull(pi30)
        assertNotNull(pi90)

        assertTrue("30-day interval width (${pi30!!.intervalWidthKg}) should be >= 7-day width (${pi7!!.intervalWidthKg})", pi30.intervalWidthKg >= pi7.intervalWidthKg)
        assertTrue("90-day interval width (${pi90!!.intervalWidthKg}) should be >= 30-day width (${pi30.intervalWidthKg})", pi90.intervalWidthKg >= pi30.intervalWidthKg)
    }
}
