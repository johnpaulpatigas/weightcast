package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import org.junit.Assert.assertEquals
import org.junit.Test

class DataSufficiencyEvaluatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `empty, 1, and 2 measurements return INSUFFICIENT_DATA`() {
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, DataSufficiencyEvaluator.evaluate(emptyList()))

        val single = listOf(PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0))
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, DataSufficiencyEvaluator.evaluate(single))

        val pair = listOf(
            PreparedMeasurement(timestamp = baseTime, elapsedDays = 0.0, weightKg = 70.0),
            PreparedMeasurement(timestamp = baseTime + dayMs, elapsedDays = 1.0, weightKg = 70.5)
        )
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, DataSufficiencyEvaluator.evaluate(pair))
    }

    @Test
    fun `3 measurements return LOW_CONFIDENCE`() {
        val threePoints = (0..2).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0)
        }
        assertEquals(DataSufficiency.LOW_CONFIDENCE, DataSufficiencyEvaluator.evaluate(threePoints))
    }

    @Test
    fun `5 measurements spanning exactly 7 days return MODERATE_CONFIDENCE`() {
        val days = listOf(0L, 2L, 4L, 6L, 7L)
        val fivePoints = days.map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0)
        }
        assertEquals(DataSufficiency.MODERATE_CONFIDENCE, DataSufficiencyEvaluator.evaluate(fivePoints))
    }

    @Test
    fun `10 measurements spanning 14 days with good coverage return HIGH_CONFIDENCE`() {
        val days = listOf(0L, 1L, 2L, 4L, 6L, 8L, 10L, 12L, 13L, 14L)
        val tenPoints = days.map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0)
        }
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, DataSufficiencyEvaluator.evaluate(tenPoints))
    }

    @Test
    fun `adequate count but inadequate time span returns LOW_CONFIDENCE`() {
        val tenPointsIn5Days = (0..9).map { idx ->
            PreparedMeasurement(timestamp = baseTime + (idx * (dayMs / 2)), elapsedDays = (idx * 0.5), weightKg = 70.0)
        }
        assertEquals(DataSufficiency.LOW_CONFIDENCE, DataSufficiencyEvaluator.evaluate(tenPointsIn5Days))
    }

    @Test
    fun `adequate count and span but sparse coverage returns LOW_CONFIDENCE`() {
        val sparseDays = listOf(0L, 10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 100L)
        val sparsePoints = sparseDays.map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 70.0)
        }
        assertEquals(DataSufficiency.LOW_CONFIDENCE, DataSufficiencyEvaluator.evaluate(sparsePoints))
    }
}
