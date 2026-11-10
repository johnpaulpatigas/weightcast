package com.johnpaulpatigas.weightcast.domain.trend.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class DoubleMedianTrackerTest {

    @Test
    fun `tracker matches calculateMedian on single element`() {
        val tracker = DoubleMedianTracker()
        tracker.add(42.0)
        assertEquals(42.0, tracker.getMedian(), 1e-9)
    }

    @Test
    fun `tracker matches calculateMedian on two elements`() {
        val tracker = DoubleMedianTracker()
        tracker.add(10.0)
        tracker.add(20.0)
        assertEquals(15.0, tracker.getMedian(), 1e-9)
    }

    @Test
    fun `tracker matches calculateMedian on odd and even stream insertions`() {
        val tracker = DoubleMedianTracker()
        val values = doubleArrayOf(1.0, 3.0, 2.0, 5.0, 4.0, 6.0, 0.5)

        val expectedMedians = doubleArrayOf(
            1.0,               // [1.0]
            2.0,               // [1.0, 3.0] -> avg(1.0, 3.0) = 2.0
            2.0,               // [1.0, 2.0, 3.0] -> 2.0
            2.5,               // [1.0, 2.0, 3.0, 5.0] -> avg(2.0, 3.0) = 2.5
            3.0,               // [1.0, 2.0, 3.0, 4.0, 5.0] -> 3.0
            3.5,               // [1.0, 2.0, 3.0, 4.0, 5.0, 6.0] -> avg(3.0, 4.0) = 3.5
            3.0                // [0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0] -> 3.0
        )

        values.indices.forEach { index ->
            tracker.add(values[index])
            assertEquals("Mismatch at index $index", expectedMedians[index], tracker.getMedian(), 1e-9)
        }
    }

    @Test
    fun `tracker produces identical medians as calculateMedian for 1000 random double streams`() {
        val tracker = DoubleMedianTracker()
        val rng = Random(42)
        val array = DoubleArray(1000)

        for (i in 0 until 1000) {
            val v = rng.nextDouble() * 100.0 - 50.0
            array[i] = v
            tracker.add(v)

            val sortedSlice = array.copyOfRange(0, i + 1)
            val expectedMedian = TheilSenEstimator.calculateMedian(sortedSlice, i + 1)

            assertEquals("Mismatch at stream length ${i + 1}", expectedMedian, tracker.getMedian(), 1e-9)
        }
    }
}
