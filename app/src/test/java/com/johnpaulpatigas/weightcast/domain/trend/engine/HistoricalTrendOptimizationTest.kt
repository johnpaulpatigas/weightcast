package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class HistoricalTrendOptimizationTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs
    private val numericalTolerance = 1e-6

    /**
     * Baseline helper that calculates exact historical trend output using the independent [StatisticalTrendEvaluator.evaluateTrend] implementation.
     */
    private fun computeBaselineHistoricalTrendMap(entries: List<WeightEntry>): Map<Long, Double?> {
        val sortedAsc = entries.sortedBy { it.timestamp }
        return sortedAsc.associate { entry ->
            val trendResult = StatisticalTrendEvaluator.evaluateTrend(
                rawEntries = sortedAsc.filter { it.timestamp <= entry.timestamp },
                evaluationTimestamp = entry.timestamp
            )
            entry.timestamp to trendResult.estimate?.currentTrendKg
        }
    }

    private fun assertIncrementalMatchesBaseline(entries: List<WeightEntry>) {
        val expectedMap = computeBaselineHistoricalTrendMap(entries)
        val timestamps = entries.map { it.timestamp }.toSet()

        val actualMap = IncrementalTheilSenEvaluator.evaluateHistoricalTrendMap(
            allEntries = entries,
            evalTimestamps = timestamps
        )

        assertEquals("Timestamp map keys count mismatch", expectedMap.keys.size, actualMap.keys.size)

        for ((ts, expectedVal) in expectedMap) {
            val actualVal = actualMap[ts]
            if (expectedVal == null) {
                assertNull("Expected null trend at timestamp $ts", actualVal)
            } else {
                assertEquals(
                    "Fitted trend mismatch at timestamp $ts",
                    expectedVal,
                    actualVal!!,
                    numericalTolerance
                )
            }
        }
    }

    @Test
    fun `1 constant weight incremental result matches independent baseline`() {
        val entries = (0..10).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 75.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `2 consistent weight loss incremental result matches independent baseline`() {
        val entries = (0..15).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 85.0 - (0.15 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `3 consistent weight gain incremental result matches independent baseline`() {
        val entries = (0..15).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 70.0 + (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `4 random measurement noise incremental result matches independent baseline`() {
        val rng = Random(42)
        var weight = 80.0
        val entries = (0..20).map { day ->
            weight += (rng.nextDouble() - 0.52) * 0.3
            WeightEntry(id = (day + 1).toLong(), weightInKg = weight, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `5 single large outlier incremental result matches independent baseline`() {
        val entries = (0..12).map { day ->
            val w = if (day == 6) 120.0 else 80.0 - (0.1 * day)
            WeightEntry(id = (day + 1).toLong(), weightInKg = w, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `6 multiple outliers incremental result matches independent baseline`() {
        val entries = (0..15).map { day ->
            val w = when (day) {
                3 -> 110.0
                9 -> 45.0
                else -> 82.0 - (0.08 * day)
            }
            WeightEntry(id = (day + 1).toLong(), weightInKg = w, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `7 irregular measurement dates incremental result matches independent baseline`() {
        val days = listOf(0, 2, 3, 7, 14, 25, 40, 60)
        val entries = days.mapIndexed { idx, day ->
            WeightEntry(id = (idx + 1).toLong(), weightInKg = 90.0 - (0.05 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `8 multiple measurements on one UTC date incremental result matches independent baseline`() {
        val sameDayTs = baseTime + (5 * dayMs)
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 79.5, originalUnit = WeightUnit.KG, timestamp = baseTime + (2 * dayMs)),
            WeightEntry(id = 3, weightInKg = 81.0, originalUnit = WeightUnit.KG, timestamp = sameDayTs + 1000),
            WeightEntry(id = 4, weightInKg = 79.0, originalUnit = WeightUnit.KG, timestamp = sameDayTs + 5000),
            WeightEntry(id = 5, weightInKg = 78.5, originalUnit = WeightUnit.KG, timestamp = baseTime + (10 * dayMs))
        )
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `9 missing dates incremental result matches independent baseline`() {
        val days = listOf(0, 1, 10, 11, 20, 21)
        val entries = days.mapIndexed { idx, day ->
            WeightEntry(id = (idx + 1).toLong(), weightInKg = 85.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `10 future dated measurements incremental result matches independent baseline`() {
        val entries = (0..10).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 80.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val evalTimeDay5 = baseTime + (5 * dayMs)
        val baselineResult = StatisticalTrendEvaluator.evaluateTrend(
            rawEntries = entries.filter { it.timestamp <= evalTimeDay5 },
            evaluationTimestamp = evalTimeDay5
        )

        val incrementalMap = IncrementalTheilSenEvaluator.evaluateHistoricalTrendMap(
            allEntries = entries,
            evalTimestamps = setOf(evalTimeDay5)
        )

        assertEquals(
            baselineResult.estimate!!.currentTrendKg,
            incrementalMap[evalTimeDay5]!!,
            numericalTolerance
        )
    }

    @Test
    fun `11 insufficient data incremental result matches independent baseline`() {
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 79.5, originalUnit = WeightUnit.KG, timestamp = baseTime + dayMs)
        )
        assertIncrementalMatchesBaseline(entries)
    }

    @Test
    fun `12 invalid regression results incremental result matches independent baseline`() {
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 5.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 4.0, originalUnit = WeightUnit.KG, timestamp = baseTime + dayMs),
            WeightEntry(id = 3, weightInKg = 3.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (2 * dayMs))
        )
        assertIncrementalMatchesBaseline(entries)
    }
}
