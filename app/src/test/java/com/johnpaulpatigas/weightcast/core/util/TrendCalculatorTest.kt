package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendCalculatorTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `calculate7DayMovingAverage with empty list returns empty`() {
        val result = TrendCalculator.calculate7DayMovingAverage(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculate7DayMovingAverage averages trailing 7 days correctly`() {
        val baseTime = 1000000000000L
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 78.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (1 * dayMs)),
            WeightEntry(id = 3, weightInKg = 76.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (2 * dayMs))
        )

        val ma = TrendCalculator.calculate7DayMovingAverage(entries)
        assertEquals(3, ma.size)
        // Point 1: avg(80.0) = 80.0
        assertEquals(80.0, ma[0].second, 0.001)
        // Point 2: avg(80.0, 78.0) = 79.0
        assertEquals(79.0, ma[1].second, 0.001)
        // Point 3: avg(80.0, 78.0, 76.0) = 78.0
        assertEquals(78.0, ma[2].second, 0.001)
    }

    @Test
    fun `calculateWeightChange calculates change over trailing days`() {
        val now = 1000000000000L
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = now - (10 * dayMs)),
            WeightEntry(id = 2, weightInKg = 77.5, originalUnit = WeightUnit.KG, timestamp = now)
        )

        val change7d = TrendCalculator.calculateWeightChange(entries, days = 7, nowMillis = now)
        assertEquals(-2.5, change7d!!, 0.001)
    }

    @Test
    fun `filterEntriesByTimeRange filters by time cutoff`() {
        val now = 1000000000000L
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = now - (40 * dayMs)), // 40 days ago
            WeightEntry(id = 2, weightInKg = 78.0, originalUnit = WeightUnit.KG, timestamp = now - (5 * dayMs))   // 5 days ago
        )

        val filtered7d = TrendCalculator.filterEntriesByTimeRange(entries, TimeRange.DAYS_7, nowMillis = now)
        assertEquals(1, filtered7d.size)
        assertEquals(78.0, filtered7d.first().weightInKg, 0.001)

        val filteredAll = TrendCalculator.filterEntriesByTimeRange(entries, TimeRange.ALL_TIME, nowMillis = now)
        assertEquals(2, filteredAll.size)
    }
}
