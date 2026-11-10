package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime

class HistoricalChartPerformanceBenchmarkTest {

    private val dayMs = 86_400_000L
    private val baseTime = 1_000_000_000_000L

    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var getStatisticalTrendUseCase: GetStatisticalTrendUseCase
    private lateinit var getHistoricalChartDataUseCase: GetHistoricalChartDataUseCase

    @Before
    fun setUp() {
        weightRepository = FakeWeightRepository()
        getStatisticalTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        getHistoricalChartDataUseCase = GetHistoricalChartDataUseCase(weightRepository, getStatisticalTrendUseCase)
    }

    @Test
    fun `benchmark complete historical chart data generation for 30 100 250 and 500 measurements`() = runTest {
        val sizes = listOf(30, 100, 250, 500)
        val benchmarkResults = mutableMapOf<Int, Double>()

        for (n in sizes) {
            weightRepository.clear()
            val rng = Random(42)
            var currentWeight = 85.0
            (0 until n).forEach { day ->
                currentWeight += (rng.nextDouble() - 0.52) * 0.2
                weightRepository.addEntry(
                    WeightEntry(
                        id = (day + 1).toLong(),
                        weightInKg = currentWeight,
                        originalUnit = WeightUnit.KG,
                        timestamp = baseTime + (day * dayMs)
                    )
                )
            }

            // Warmup
            getHistoricalChartDataUseCase(TimeRange.ALL_TIME).first()

            // Measured run
            val elapsedNanos = measureNanoTime {
                val points = getHistoricalChartDataUseCase(TimeRange.ALL_TIME).first()
                assertEquals(n, points.size)
            }

            val elapsedMillis = elapsedNanos / 1_000_000.0
            benchmarkResults[n] = elapsedMillis
            println("Benchmark: N=$n measurements -> Complete Chart Generation Time = %.3f ms".format(elapsedMillis))
        }

        sizes.forEach { size ->
            assertTrue("Chart calculation for $size measurements completed", benchmarkResults[size]!! >= 0.0)
        }
    }
}
