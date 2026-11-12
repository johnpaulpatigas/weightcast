package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendCalculatorTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `filterEntriesByTimeRange filters by time cutoff`() {
        val now = 1000000000000L
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = now - (40 * dayMs)), // 40 days ago
            WeightEntry(id = 2, weightInKg = 78.0, originalUnit = WeightUnit.KG, timestamp = now - (5 * dayMs))   // 5 days ago
        )

        val filtered7d = TrendCalculator.filterEntriesByTimeRange(entries, TimeRange.WEEKS_1, nowMillis = now)
        assertEquals(1, filtered7d.size)
        assertEquals(78.0, filtered7d.first().weightInKg, 0.001)

        val filteredAll = TrendCalculator.filterEntriesByTimeRange(entries, TimeRange.ALL_TIME, nowMillis = now)
        assertEquals(2, filteredAll.size)
    }
}
