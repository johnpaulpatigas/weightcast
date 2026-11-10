package com.johnpaulpatigas.weightcast.domain.forecast.engine

import com.johnpaulpatigas.weightcast.domain.trend.engine.PreparedMeasurement
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import com.johnpaulpatigas.weightcast.domain.trend.model.WeightTrendEstimate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class BootstrapComparisonTest {

    private val dayMs = 86_400_000L
    private val baseTs = 100 * dayMs

    private fun generateAutocorrelatedDataset(count: Int, rho: Double = 0.6, noiseStd: Double = 0.3, seed: Long = 42L): List<PreparedMeasurement> {
        val rng = Random(seed)
        var prevNoise = 0.0
        return (0 until count).map { day ->
            val innov = rng.nextGaussian() * noiseStd
            val noise = (rho * prevNoise) + innov
            prevNoise = noise
            PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = 80.0 - (0.05 * day) + noise,
                elapsedDays = day.toDouble()
            )
        }
    }

    private fun generateHeavyTailedDataset(count: Int, seed: Long = 42L): List<PreparedMeasurement> {
        val rng = Random(seed)
        return (0 until count).map { day ->
            // Student's t distribution with df=3
            val z = rng.nextGaussian()
            val v = (rng.nextDouble() + rng.nextDouble() + rng.nextDouble()) / 3.0
            val tNoise = z / kotlin.math.sqrt(maxOf(0.1, v)) * 0.2
            PreparedMeasurement(
                timestamp = baseTs + (day * dayMs),
                weightKg = 80.0 - (0.05 * day) + tNoise,
                elapsedDays = day.toDouble()
            )
        }
    }

    @Test
    fun `moving block bootstrap handles autocorrelated noise without underestimating interval width`() {
        val measurements = generateAutocorrelatedDataset(count = 20, rho = 0.7, noiseStd = 0.3)
        val evalTs = baseTs + (19 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.05,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 20,
            timeSpanDays = 19.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.3
        )

        val piIndep = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 30.0,
            bootstrapIterations = 300,
            bootstrapMethod = BootstrapMethod.INDEPENDENT_RESIDUAL,
            randomSeed = 42L
        )

        val piBlock = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 30.0,
            bootstrapIterations = 300,
            bootstrapMethod = BootstrapMethod.MOVING_BLOCK_RESIDUAL,
            blockSize = 3,
            randomSeed = 42L
        )

        assertNotNull(piIndep)
        assertNotNull(piBlock)
        assertTrue("Moving block width (${piBlock!!.intervalWidthKg}) should account for block autocorrelation", piBlock.intervalWidthKg >= piIndep!!.intervalWidthKg * 0.8)
    }

    @Test
    fun `moving block bootstrap handles heavy tailed noise accurately`() {
        val measurements = generateHeavyTailedDataset(count = 20)
        val evalTs = baseTs + (19 * dayMs)
        val estimate = WeightTrendEstimate(
            currentTrendKg = 79.05,
            slopeKgPerDay = -0.05,
            interceptKg = 80.0,
            measurementCount = 20,
            timeSpanDays = 19.0,
            dataSufficiency = DataSufficiency.HIGH_CONFIDENCE,
            trendStability = TrendStability.STABLE,
            residualStdDevKg = 0.3
        )

        val piBlock = PredictionIntervalEstimator.estimate(
            preparedMeasurements = measurements,
            estimate = estimate,
            evaluationTimestamp = evalTs,
            horizonDays = 7.0,
            bootstrapIterations = 300,
            bootstrapMethod = BootstrapMethod.MOVING_BLOCK_RESIDUAL,
            randomSeed = 42L
        )

        assertNotNull(piBlock)
        val pointEst = estimate.interceptKg + (estimate.slopeKgPerDay * (19.0 + 7.0))
        assertTrue(piBlock!!.lowerBoundKg <= pointEst)
        assertTrue(pointEst <= piBlock.upperBoundKg)
    }
}
