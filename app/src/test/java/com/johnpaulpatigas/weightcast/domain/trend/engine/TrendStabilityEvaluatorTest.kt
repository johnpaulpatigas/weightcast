package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendStabilityEvaluatorTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    @Test
    fun `perfect linear weight loss is STABLE with zero subwindow slope difference`() {
        val measurements = (0..10).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 80.0 - (0.1 * day))
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        val (stability, diagnostics) = TrendStabilityEvaluator.evaluate(measurements, estimate)

        assertEquals(TrendStability.STABLE, stability)
        assertNotNull(diagnostics)
        assertEquals(0.0, diagnostics!!.slopeDifferenceKgPerWeek!!, 0.001)
        assertTrue(!diagnostics.isDirectionReversal)
    }

    @Test
    fun `genuine direction reversal is UNSTABLE and flags isDirectionReversal`() {
        // First 5 days: loss at -0.5 kg/week (-0.0714 kg/day), initial 80kg -> 79.64kg
        // Next 5 days: gain at +0.5 kg/week (+0.0714 kg/day), 79.64kg -> 80.0kg
        val measurements = (0..10).map { day ->
            val weight = if (day <= 5) {
                80.0 - (0.0714 * day)
            } else {
                79.643 + (0.0714 * (day - 5))
            }
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = weight)
        }

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (10 * dayMs))
        val (stability, diagnostics) = TrendStabilityEvaluator.evaluate(measurements, estimate)

        assertEquals(TrendStability.UNSTABLE, stability)
        assertNotNull(diagnostics)
        assertTrue(diagnostics!!.isDirectionReversal)
    }

    @Test
    fun `strongly different subwindow slopes evaluate to UNSTABLE`() {
        // First half: gentle loss (-0.1 kg/week), Second half: steep loss (-1.2 kg/week)
        val firstHalfDays = (0..4).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 80.0 - (0.014 * day))
        }
        val secondHalfDays = (5..9).map { day ->
            PreparedMeasurement(timestamp = baseTime + (day * dayMs), elapsedDays = day.toDouble(), weightKg = 79.93 - (0.171 * (day - 5)))
        }
        val measurements = firstHalfDays + secondHalfDays

        val estimate = TheilSenEstimator.estimate(measurements, evaluationTimestamp = baseTime + (9 * dayMs))
        val (stability, diagnostics) = TrendStabilityEvaluator.evaluate(measurements, estimate)

        assertEquals(TrendStability.UNSTABLE, stability)
        assertNotNull(diagnostics)
        assertTrue(diagnostics!!.slopeDifferenceKgPerWeek!! > 0.75)
    }
}
