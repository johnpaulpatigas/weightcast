package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class SyntheticNoiseStressTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    private fun generateSyntheticDataset(
        daysCount: Int,
        baseWeightKg: Double,
        dailySlopeKg: Double,
        noiseStdDevKg: Double,
        seed: Long = 42L
    ): List<WeightEntry> {
        val rng = Random(seed)
        return (0 until daysCount).map { day ->
            val noise = if (noiseStdDevKg > 0.0) rng.nextGaussian() * noiseStdDevKg else 0.0
            val weight = baseWeightKg + (dailySlopeKg * day) + noise
            WeightEntry(
                id = (day + 1).toLong(),
                weightInKg = weight,
                originalUnit = WeightUnit.KG,
                timestamp = baseTime + (day * dayMs)
            )
        }
    }

    @Test
    fun `1 linear loss with 0_1 kg noise estimates slope accurately and remains STABLE`() {
        val trueWeeklyChange = -0.5 // -0.5 kg/week
        val dailySlope = trueWeeklyChange / 7.0
        val entries = generateSyntheticDataset(daysCount = 14, baseWeightKg = 80.0, dailySlopeKg = dailySlope, noiseStdDevKg = 0.1)

        val evalTime = baseTime + (13 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.05)
        assertEquals(TrendStability.STABLE, estimate.trendStability)
        assertTrue(estimate.residualStdDevKg <= 0.3)
    }

    @Test
    fun `2 linear loss with 0_3 kg noise estimates slope accurately`() {
        val trueWeeklyChange = -0.5
        val dailySlope = trueWeeklyChange / 7.0
        val entries = generateSyntheticDataset(daysCount = 14, baseWeightKg = 80.0, dailySlopeKg = dailySlope, noiseStdDevKg = 0.3)

        val evalTime = baseTime + (13 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.10)
    }

    @Test
    fun `3 linear loss with 0_5 kg noise estimates slope accurately`() {
        val trueWeeklyChange = -0.5
        val dailySlope = trueWeeklyChange / 7.0
        val entries = generateSyntheticDataset(daysCount = 20, baseWeightKg = 85.0, dailySlopeKg = dailySlope, noiseStdDevKg = 0.5)

        val evalTime = baseTime + (19 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.15)
    }

    @Test
    fun `4 linear loss with 1_0 kg noise flags higher residual scatter`() {
        val trueWeeklyChange = -0.5
        val dailySlope = trueWeeklyChange / 7.0
        val entries = generateSyntheticDataset(daysCount = 20, baseWeightKg = 85.0, dailySlopeKg = dailySlope, noiseStdDevKg = 1.0)

        val evalTime = baseTime + (19 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertTrue(estimate.residualStdDevKg > 0.5)
    }

    @Test
    fun `5 linear gain with 0_3 kg noise estimates gain slope accurately`() {
        val trueWeeklyChange = 0.5 // +0.5 kg/week
        val dailySlope = trueWeeklyChange / 7.0
        val entries = generateSyntheticDataset(daysCount = 14, baseWeightKg = 70.0, dailySlopeKg = dailySlope, noiseStdDevKg = 0.3)

        val evalTime = baseTime + (13 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.10)
    }

    @Test
    fun `6 constant weight with 0_2 kg noise estimates zero slope`() {
        val entries = generateSyntheticDataset(daysCount = 14, baseWeightKg = 75.0, dailySlopeKg = 0.0, noiseStdDevKg = 0.2)

        val evalTime = baseTime + (13 * dayMs)
        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = evalTime)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(0.0, estimate.weeklyChangeKg, 0.10)
        assertTrue(estimate.trendStability != TrendStability.UNSTABLE)
    }

    @Test
    fun `7 single extreme outlier does not distort Theil-Sen slope`() {
        val trueWeeklyChange = -0.7
        val dailySlope = trueWeeklyChange / 7.0
        val raw = (0..13).map { day ->
            val spike = if (day == 6) 15.0 else 0.0
            WeightEntry(id = (day + 1).toLong(), weightInKg = 90.0 + (dailySlope * day) + spike, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(raw, evaluationTimestamp = baseTime + (13 * dayMs))
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.05)
    }

    @Test
    fun `8 multiple extreme outliers maintain slope robustness`() {
        val trueWeeklyChange = -0.5
        val dailySlope = trueWeeklyChange / 7.0
        val raw = (0..13).map { day ->
            val spike = when (day) {
                3 -> 20.0
                10 -> -15.0
                else -> 0.0
            }
            WeightEntry(id = (day + 1).toLong(), weightInKg = 85.0 + (dailySlope * day) + spike, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(raw, evaluationTimestamp = baseTime + (13 * dayMs))
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(trueWeeklyChange, estimate.weeklyChangeKg, 0.10)
    }

    @Test
    fun `9 abrupt direction reversal flags UNSTABLE with direction reversal true`() {
        val lossEntries = (0..6).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        val gainEntries = (7..13).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 79.3 + (0.1 * (day - 6)), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(lossEntries + gainEntries, evaluationTimestamp = baseTime + (13 * dayMs))
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(TrendStability.UNSTABLE, estimate.trendStability)
        assertTrue(result.diagnostics.stabilityDiagnostics?.isDirectionReversal == true)
    }

    @Test
    fun `10 sparse irregular measurements handled cleanly`() {
        val days = listOf(0L, 3L, 10L, 15L, 28L)
        val entries = days.mapIndexed { idx, day ->
            WeightEntry(id = (idx + 1).toLong(), weightInKg = 70.0 + (0.05 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (28 * dayMs))
        assertNotNull(result.estimate)
        assertEquals(0.35, result.estimate!!.weeklyChangeKg, 0.05)
    }
}
