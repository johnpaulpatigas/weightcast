package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPreparationTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `prepare with empty entries returns empty dataset`() {
        val result = DataPreparation.prepare(emptyList())
        assertTrue(result.measurements.isEmpty())
        assertEquals(0, result.diagnostics.acceptedRawCount)
        assertEquals(0, result.diagnostics.rejectedRawCount)
    }

    @Test
    fun `prepare sorts entries chronologically and normalizes elapsed days`() {
        val unordered = listOf(
            WeightEntry(id = 1, weightInKg = 72.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (3 * dayMs)),
            WeightEntry(id = 2, weightInKg = 75.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 3, weightInKg = 73.5, originalUnit = WeightUnit.KG, timestamp = baseTime + (1 * dayMs))
        )

        val prepared = DataPreparation.prepare(unordered).measurements
        assertEquals(3, prepared.size)
        assertEquals(75.0, prepared[0].weightKg, 0.001)
        assertEquals(0.0, prepared[0].elapsedDays, 0.001)

        assertEquals(73.5, prepared[1].weightKg, 0.001)
        assertEquals(1.0, prepared[1].elapsedDays, 0.001)

        assertEquals(72.0, prepared[2].weightKg, 0.001)
        assertEquals(3.0, prepared[2].elapsedDays, 0.001)
    }

    @Test
    fun `prepare aggregates multiple daily measurements into arithmetic mean`() {
        val sameDayEntries = listOf(
            WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime + 1000L),
            WeightEntry(id = 2, weightInKg = 82.0, originalUnit = WeightUnit.KG, timestamp = baseTime + 5000L)
        )

        val result = DataPreparation.prepare(sameDayEntries)
        assertEquals(1, result.measurements.size)
        assertEquals(81.0, result.measurements.first().weightKg, 0.001)
        assertEquals(2, result.measurements.first().originalMeasurementCount)
        assertEquals(2, result.diagnostics.acceptedRawCount)
        assertEquals(1, result.diagnostics.aggregatedDailyCount)
    }

    @Test
    fun `prepare rejects invalid weights below 2kg or above 500kg`() {
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 0.5, originalUnit = WeightUnit.KG, timestamp = baseTime), // Invalid
            WeightEntry(id = 2, weightInKg = 70.0, originalUnit = WeightUnit.KG, timestamp = baseTime + dayMs), // Valid
            WeightEntry(id = 3, weightInKg = 600.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (2 * dayMs)) // Invalid
        )

        val result = DataPreparation.prepare(entries)
        assertEquals(1, result.measurements.size)
        assertEquals(70.0, result.measurements.first().weightKg, 0.001)
        assertEquals(2, result.diagnostics.rejectedRawCount)
        assertEquals(1, result.diagnostics.acceptedRawCount)
    }

    @Test
    fun `prepare does not fabricate missing days`() {
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 70.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 68.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (10 * dayMs))
        )

        val prepared = DataPreparation.prepare(entries).measurements
        assertEquals(2, prepared.size)
        assertEquals(0.0, prepared[0].elapsedDays, 0.001)
        assertEquals(10.0, prepared[1].elapsedDays, 0.001)
    }
}
