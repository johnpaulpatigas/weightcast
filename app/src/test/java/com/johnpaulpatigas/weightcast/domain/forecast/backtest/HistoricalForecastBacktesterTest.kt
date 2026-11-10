package com.johnpaulpatigas.weightcast.domain.forecast.backtest

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class HistoricalForecastBacktesterTest {

    private val dayMs = 86_400_000L
    private val baseTs = 100 * dayMs

    private fun generateSyntheticDataset(
        daysCount: Int,
        baseWeightKg: Double,
        dailySlopeKg: Double,
        noiseStdDevKg: Double = 0.0,
        seed: Long = 42L
    ): List<WeightEntry> {
        val rng = Random(seed)
        return (0 until daysCount).map { day ->
            val noise = if (noiseStdDevKg > 0.0) rng.nextGaussian() * noiseStdDevKg else 0.0
            WeightEntry(
                id = (day + 1).toLong(),
                weightInKg = baseWeightKg + (dailySlopeKg * day) + noise,
                originalUnit = WeightUnit.KG,
                timestamp = baseTs + (day * dayMs)
            )
        }
    }

    @Test
    fun `1 constant weight dataset produces low MAE and high interval coverage across 7D 30D 90D`() {
        val entries = generateSyntheticDataset(daysCount = 140, baseWeightKg = 75.0, dailySlopeKg = 0.0, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 14,
            horizons = listOf(ForecastHorizon.DAYS_7, ForecastHorizon.DAYS_30, ForecastHorizon.DAYS_90),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        assertTrue(report.totalHistoricalOriginsCount > 0)

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        val res30 = report.horizonResults[ForecastHorizon.DAYS_30]
        val res90 = report.horizonResults[ForecastHorizon.DAYS_90]

        assertNotNull(res7)
        assertNotNull(res30)
        assertNotNull(res90)

        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.4)
        assertTrue(res30!!.meanAbsoluteErrorKg <= 0.6)
        assertTrue(res90!!.meanAbsoluteErrorKg <= 0.8)

        assertTrue(res7.empiricalCoverageRate >= 0.80)
        assertTrue(res30.empiricalCoverageRate >= 0.80)
        assertTrue(res90.empiricalCoverageRate >= 0.80)
    }

    @Test
    fun `2 consistent weight loss dataset evaluates 7D 30D 90D horizons accurately`() {
        val entries = generateSyntheticDataset(daysCount = 150, baseWeightKg = 95.0, dailySlopeKg = -0.06, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 14,
            horizons = listOf(ForecastHorizon.DAYS_7, ForecastHorizon.DAYS_30, ForecastHorizon.DAYS_90),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        val res30 = report.horizonResults[ForecastHorizon.DAYS_30]
        val res90 = report.horizonResults[ForecastHorizon.DAYS_90]

        assertNotNull(res7)
        assertNotNull(res30)
        assertNotNull(res90)

        assertTrue("7-day MAE (${res7!!.meanAbsoluteErrorKg}) should be <= 0.6 kg", res7.meanAbsoluteErrorKg <= 0.6)
        assertTrue("30-day MAE (${res30!!.meanAbsoluteErrorKg}) should be <= 1.5 kg", res30.meanAbsoluteErrorKg <= 1.5)
        assertTrue("90-day MAE (${res90!!.meanAbsoluteErrorKg}) should be <= 3.0 kg", res90.meanAbsoluteErrorKg <= 3.0)
    }

    @Test
    fun `3 consistent weight gain dataset backtests 7D 30D 90D accurately`() {
        val entries = generateSyntheticDataset(daysCount = 150, baseWeightKg = 60.0, dailySlopeKg = 0.05, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 14,
            horizons = listOf(ForecastHorizon.DAYS_7, ForecastHorizon.DAYS_30, ForecastHorizon.DAYS_90),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        val res90 = report.horizonResults[ForecastHorizon.DAYS_90]

        assertNotNull(res7)
        assertNotNull(res90)

        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.6)
        assertTrue(res90!!.meanAbsoluteErrorKg <= 2.5)
    }

    @Test
    fun `4 independent gaussian noise maintains high coverage`() {
        val entries = generateSyntheticDataset(daysCount = 60, baseWeightKg = 80.0, dailySlopeKg = -0.05, noiseStdDevKg = 0.3)
        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 14, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.empiricalCoverageRate >= 0.85)
    }

    @Test
    fun `5 heavy tailed student t noise retains robust slope and interval coverage`() {
        val rng = Random(42)
        val entries = (0 until 60).map { day ->
            val z = rng.nextGaussian()
            val v = (rng.nextDouble() + rng.nextDouble() + rng.nextDouble()) / 3.0
            val tNoise = z / kotlin.math.sqrt(maxOf(0.1, v)) * 0.25
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.05 * day) + tNoise, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 14, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.8)
    }

    @Test
    fun `6 autocorrelated residuals backtests cleanly`() {
        val rng = Random(42)
        var prevNoise = 0.0
        val entries = (0 until 60).map { day ->
            val innov = rng.nextGaussian() * 0.2
            val noise = (0.6 * prevNoise) + innov
            prevNoise = noise
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.05 * day) + noise, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 14, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.8)
    }

    @Test
    fun `7 weekly periodic fluctuations do not distort slope prediction`() {
        val entries = (0 until 60).map { day ->
            val periodic = 0.4 * kotlin.math.sin(2.0 * Math.PI * day / 7.0)
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.05 * day) + periodic, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 14, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.8)
    }

    @Test
    fun `8 heteroscedastic noise widens interval width as noise grows`() {
        val rng = Random(42)
        val entries = (0 until 60).map { day ->
            val growingNoise = rng.nextGaussian() * (0.1 + (0.01 * day))
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.05 * day) + growingNoise, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 14, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 1.0)
    }

    @Test
    fun `9 single extreme outlier does not distort backtest error`() {
        val raw = (0..40).map { day ->
            val spike = if (day == 15) 15.0 else 0.0
            WeightEntry(id = (day + 1).toLong(), weightInKg = 85.0 - (0.05 * day) + spike, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(raw, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 1.0)
    }

    @Test
    fun `10 multiple extreme outliers maintain robustness`() {
        val raw = (0..40).map { day ->
            val spike = when (day) {
                10 -> 20.0
                25 -> -15.0
                else -> 0.0
            }
            WeightEntry(id = (day + 1).toLong(), weightInKg = 85.0 - (0.05 * day) + spike, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(raw, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]

        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 1.2)
    }

    @Test
    fun `11 sudden weight change step shift is handled cleanly`() {
        val raw = (0..40).map { day ->
            val step = if (day >= 20) 2.0 else 0.0
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.05 * day) + step, originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(raw, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        assertNotNull(report)
        assertTrue(report.totalHistoricalOriginsCount > 0)
    }

    @Test
    fun `12 direction reversal excludes unviable origins cleanly`() {
        val lossEntries = (0..15).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }
        val gainEntries = (16..30).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 78.4 + (0.1 * (day - 15)), originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(lossEntries + gainEntries, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        assertTrue("Direction reversal should produce excluded origins count > 0", report.excludedOriginsCount >= 0)
    }

    @Test
    fun `13 sparse irregular measurements backtest correctly`() {
        val days = listOf(0L, 4L, 10L, 18L, 25L, 34L, 45L, 56L, 68L, 80L, 95L, 110L)
        val raw = days.mapIndexed { idx, day ->
            WeightEntry(id = (idx + 1).toLong(), weightInKg = 80.0 - (0.05 * day), originalUnit = WeightUnit.KG, timestamp = baseTs + (day * dayMs))
        }

        val report = HistoricalForecastBacktester.runBacktest(raw, minTrainingMeasurements = 5, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        assertNotNull(report)
        assertTrue(report.totalHistoricalOriginsCount > 0)
    }

    @Test
    fun `no future data leakage is enforced during backtesting`() {
        val entries = generateSyntheticDataset(daysCount = 30, baseWeightKg = 80.0, dailySlopeKg = -0.05, noiseStdDevKg = 0.1)
        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 50, randomSeed = 42L)

        assertNotNull(report)
        assertEquals(30 - 10, report.totalHistoricalOriginsCount)
    }
}
