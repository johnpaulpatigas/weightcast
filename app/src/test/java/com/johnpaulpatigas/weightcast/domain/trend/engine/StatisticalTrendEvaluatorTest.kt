package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticalTrendEvaluatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `evaluateTrend with empty entries returns empty result`() {
        val result = StatisticalTrendEvaluator.evaluateTrend(emptyList())
        assertNull(result.estimate)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, result.sufficiency)
    }

    @Test
    fun `evaluateTrend with valid history calculates estimate and diagnostics`() {
        val entries = (0..14).map { day ->
            WeightEntry(id = day.toLong(), weightInKg = 80.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (14 * dayMs))
        assertTrue(result.isAvailable)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(-0.7, estimate.weeklyChangeKg, 0.001)
        assertEquals(78.6, estimate.currentTrendKg, 0.001)
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.STABLE, estimate.trendStability)

        val diagnostics = result.diagnostics
        assertEquals(15, diagnostics.validMeasurementCount)
        assertEquals(14.0, diagnostics.timeSpanDays, 0.001)
        assertNotNull(diagnostics.stabilityDiagnostics)
    }
}
