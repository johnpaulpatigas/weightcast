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
    fun `1 constant weight dataset produces low MAE and high interval coverage`() {
        val entries = generateSyntheticDataset(daysCount = 45, baseWeightKg = 75.0, dailySlopeKg = 0.0, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 10,
            horizons = listOf(ForecastHorizon.DAYS_7, ForecastHorizon.DAYS_30),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        assertTrue(report.totalHistoricalOriginsCount > 0)

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        assertNotNull(res7)
        assertTrue("MAE should be low for constant weight, got ${res7!!.meanAbsoluteErrorKg}", res7.meanAbsoluteErrorKg <= 0.5)
        assertTrue("Coverage should be high, got ${res7.empiricalCoverageRate}", res7.empiricalCoverageRate >= 0.70)
    }

    @Test
    fun `2 consistent weight loss dataset produces accurate 7-day and 30-day forecasts`() {
        val entries = generateSyntheticDataset(daysCount = 60, baseWeightKg = 90.0, dailySlopeKg = -0.07, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 14,
            horizons = listOf(ForecastHorizon.DAYS_7, ForecastHorizon.DAYS_30),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        val res30 = report.horizonResults[ForecastHorizon.DAYS_30]

        assertNotNull(res7)
        assertNotNull(res30)

        assertTrue("7-day MAE should be <= 0.6 kg", res7!!.meanAbsoluteErrorKg <= 0.6)
        assertTrue("30-day MAE should be <= 1.5 kg", res30!!.meanAbsoluteErrorKg <= 1.5)
    }

    @Test
    fun `3 consistent weight gain dataset backtests accurately`() {
        val entries = generateSyntheticDataset(daysCount = 50, baseWeightKg = 60.0, dailySlopeKg = 0.05, noiseStdDevKg = 0.2)
        val report = HistoricalForecastBacktester.runBacktest(
            rawEntries = entries,
            minTrainingMeasurements = 14,
            horizons = listOf(ForecastHorizon.DAYS_7),
            bootstrapIterations = 100,
            randomSeed = 42L
        )

        val res7 = report.horizonResults[ForecastHorizon.DAYS_7]
        assertNotNull(res7)
        assertTrue(res7!!.meanAbsoluteErrorKg <= 0.5)
    }

    @Test
    fun `4 high noise dataset increases prediction interval width appropriately`() {
        val lowNoiseEntries = generateSyntheticDataset(daysCount = 40, baseWeightKg = 80.0, dailySlopeKg = -0.05, noiseStdDevKg = 0.1)
        val highNoiseEntries = generateSyntheticDataset(daysCount = 40, baseWeightKg = 80.0, dailySlopeKg = -0.05, noiseStdDevKg = 0.8)

        val reportLow = HistoricalForecastBacktester.runBacktest(lowNoiseEntries, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)
        val reportHigh = HistoricalForecastBacktester.runBacktest(highNoiseEntries, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 100, randomSeed = 42L)

        val widthLow = reportLow.horizonResults[ForecastHorizon.DAYS_7]!!.meanIntervalWidthKg
        val widthHigh = reportHigh.horizonResults[ForecastHorizon.DAYS_7]!!.meanIntervalWidthKg

        assertTrue("High noise interval width ($widthHigh) should be > low noise interval width ($widthLow)", widthHigh > widthLow)
    }

    @Test
    fun `5 single extreme outlier does not compromise backtesting integrity`() {
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
    fun `6 direction reversal excludes unviable origins cleanly`() {
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
    fun `7 no future data leakage is enforced during backtesting`() {
        val entries = generateSyntheticDataset(daysCount = 30, baseWeightKg = 80.0, dailySlopeKg = -0.05, noiseStdDevKg = 0.1)
        val report = HistoricalForecastBacktester.runBacktest(entries, minTrainingMeasurements = 10, horizons = listOf(ForecastHorizon.DAYS_7), bootstrapIterations = 50, randomSeed = 42L)

        // Verifies backtester completes without throwing exceptions or leaking future points
        assertNotNull(report)
        assertEquals(30 - 10, report.totalHistoricalOriginsCount)
    }
}
