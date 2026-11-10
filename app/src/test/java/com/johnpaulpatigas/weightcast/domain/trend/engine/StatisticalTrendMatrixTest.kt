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
import java.util.Random

class StatisticalTrendMatrixTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `independence combination 1 - High sufficiency and Stable trend`() {
        val entries = (0..14).map { day ->
            WeightEntry(id = day.toLong(), weightInKg = 80.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (14 * dayMs))
        assertNotNull(result.estimate)
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.STABLE, result.estimate!!.trendStability)
    }

    @Test
    fun `independence combination 2 - High sufficiency and Unstable trend`() {
        // 30 daily weigh-ins with alternating scale fluctuations (+- 3kg)
        val rng = Random(42) // Fixed seed for deterministic test
        val entries = (0..30).map { day ->
            val noise = if (day % 2 == 0) 3.0 else -3.0
            WeightEntry(id = day.toLong(), weightInKg = 80.0 + noise, originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (30 * dayMs))
        assertNotNull(result.estimate)
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.UNSTABLE, result.estimate!!.trendStability)
    }

    @Test
    fun `independence combination 3 - Low sufficiency and Stable-looking trend`() {
        // 3 consecutive daily weigh-ins (linear loss)
        val entries = (0..2).map { day ->
            WeightEntry(id = day.toLong(), weightInKg = 75.0 - (0.1 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (2 * dayMs))
        assertNotNull(result.estimate)
        assertEquals(DataSufficiency.LOW_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.STABLE, result.estimate!!.trendStability)
    }

    @Test
    fun `independence combination 4 - Low sufficiency and Unstable trend`() {
        // 4 weigh-ins with extreme spike
        val entries = listOf(
            WeightEntry(id = 1, weightInKg = 75.0, originalUnit = WeightUnit.KG, timestamp = baseTime),
            WeightEntry(id = 2, weightInKg = 90.0, originalUnit = WeightUnit.KG, timestamp = baseTime + dayMs), // spike
            WeightEntry(id = 3, weightInKg = 74.8, originalUnit = WeightUnit.KG, timestamp = baseTime + (2 * dayMs)),
            WeightEntry(id = 4, weightInKg = 60.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (3 * dayMs))  // drop
        )

        val result = StatisticalTrendEvaluator.evaluateTrend(entries, evaluationTimestamp = baseTime + (3 * dayMs))
        assertNotNull(result.estimate)
        assertEquals(DataSufficiency.LOW_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.UNSTABLE, result.estimate!!.trendStability)
    }
}
