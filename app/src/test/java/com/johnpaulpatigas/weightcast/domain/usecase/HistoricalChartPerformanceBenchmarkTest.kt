package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.engine.IncrementalTheilSenEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalChartPerformanceBenchmarkTest {

    private val dayMs = 86_400_000L
    private val baseTime = 1_000_000_000_000L

    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var getStatisticalTrendUseCase: GetStatisticalTrendUseCase
    private lateinit var generateWeightForecastUseCase: GenerateWeightForecastUseCase
    private lateinit var getHistoricalChartDataUseCase: GetHistoricalChartDataUseCase

    @Before
    fun setUp() {
        weightRepository = FakeWeightRepository()
        goalRepository = FakeGoalRepository()
        getStatisticalTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        generateWeightForecastUseCase = GenerateWeightForecastUseCase(weightRepository, goalRepository, getStatisticalTrendUseCase)
        getHistoricalChartDataUseCase = GetHistoricalChartDataUseCase(
            weightRepository = weightRepository,
            generateWeightForecastUseCase = generateWeightForecastUseCase,
            defaultDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `benchmark complete historical chart data generation for 30 100 250 and 500 measurements`() = runTest {
        val sizes = listOf(30, 100, 250, 500)
        val warmupIterations = 5
        val measuredIterations = 10
        val medianResults = mutableMapOf<Int, Double>()

        println("=========================================================================")
        println("HISTORICAL CHART PERFORMANCE BENCHMARK (Phase 3.6 Optimized Implementation)")
        println("=========================================================================")

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

            // Warmup iterations
            repeat(warmupIterations) {
                getHistoricalChartDataUseCase(TimeRange.ALL_TIME, includeForecasts = false).first()
            }

            // Repeated measurement iterations
            val runTimesMs = DoubleArray(measuredIterations)
            repeat(measuredIterations) { iter ->
                val elapsedNanos = measureNanoTime {
                    val points = getHistoricalChartDataUseCase(TimeRange.ALL_TIME, includeForecasts = false).first()
                    assertEquals(n, points.size)
                }
                runTimesMs[iter] = elapsedNanos / 1_000_000.0
            }

            runTimesMs.sort()
            val medianMs = if (measuredIterations % 2 == 1) {
                runTimesMs[measuredIterations / 2]
            } else {
                (runTimesMs[measuredIterations / 2 - 1] + runTimesMs[measuredIterations / 2]) / 2.0
            }

            medianResults[n] = medianMs
            println("N = %3d measurements | Median Execution Time = %7.3f ms".format(n, medianMs))
        }

        println("=========================================================================")

        // Benchmark standalone algorithm separately from repository flow overhead
        val sampleEntries = (0 until 500).map { day ->
            WeightEntry(id = (day + 1).toLong(), weightInKg = 85.0 - (0.05 * day), originalUnit = WeightUnit.KG, timestamp = baseTime + (day * dayMs))
        }
        repeat(5) {
            IncrementalTheilSenEvaluator.evaluateHistoricalChartPoints(sampleEntries, sampleEntries)
        }
        val standaloneNanos = measureNanoTime {
            repeat(10) {
                IncrementalTheilSenEvaluator.evaluateHistoricalChartPoints(sampleEntries, sampleEntries)
            }
        }
        val standaloneMedianMs = (standaloneNanos / 10.0) / 1_000_000.0
        println("Standalone N=500 algorithm execution time: %.3f ms".format(standaloneMedianMs))
        println("=========================================================================")

        // Assert performance target: 500 measurements completes well within 200 ms
        assertTrue(
            "500 measurements chart generation median time (${medianResults[500]} ms) should be < 200 ms",
            medianResults[500]!! < 200.0
        )
    }
}
