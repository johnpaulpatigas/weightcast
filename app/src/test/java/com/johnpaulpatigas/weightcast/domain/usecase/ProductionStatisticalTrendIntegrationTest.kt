package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionStatisticalTrendIntegrationTest {

    private val dayMs = 86_400_000L
    private val baseTime = 100 * dayMs

    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var getStatisticalTrendUseCase: GetStatisticalTrendUseCase
    private lateinit var generateWeightForecastUseCase: GenerateWeightForecastUseCase
    private lateinit var getDashboardSummaryUseCase: GetDashboardSummaryUseCase
    private lateinit var getHistoricalChartDataUseCase: GetHistoricalChartDataUseCase

    @Before
    fun setUp() {
        weightRepository = FakeWeightRepository()
        goalRepository = FakeGoalRepository()
        getStatisticalTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        generateWeightForecastUseCase = GenerateWeightForecastUseCase(weightRepository, goalRepository, getStatisticalTrendUseCase)
        getDashboardSummaryUseCase = GetDashboardSummaryUseCase(weightRepository, goalRepository, getStatisticalTrendUseCase, generateWeightForecastUseCase)
        getHistoricalChartDataUseCase = GetHistoricalChartDataUseCase(weightRepository, generateWeightForecastUseCase)
    }

    @Test
    fun `GetStatisticalTrendUseCase with no measurements returns unavailable result`() = runTest {
        val result = getStatisticalTrendUseCase().first()
        assertNull(result.estimate)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, result.sufficiency)
    }

    @Test
    fun `GetStatisticalTrendUseCase with 1 measurement returns unavailable result`() = runTest {
        weightRepository.addEntry(WeightEntry(weightInKg = 75.0, originalUnit = WeightUnit.KG, timestamp = baseTime))
        val result = getStatisticalTrendUseCase().first()
        assertNull(result.estimate)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, result.sufficiency)
    }

    @Test
    fun `GetStatisticalTrendUseCase with 14 daily weigh-ins calculates trend and weekly change`() = runTest {
        val evalTime = baseTime + (14 * dayMs)
        (0..14).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 80.0 - (0.1 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTime + (day * dayMs)
                )
            )
        }

        val result = getStatisticalTrendUseCase(evalTime).first()
        assertTrue(result.isAvailable)
        assertNotNull(result.estimate)

        val estimate = result.estimate!!
        assertEquals(78.6, estimate.currentTrendKg, 0.001)
        assertEquals(-0.7, estimate.weeklyChangeKg, 0.001)
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, result.sufficiency)
        assertEquals(TrendStability.STABLE, estimate.trendStability)
    }

    @Test
    fun `GetStatisticalTrendUseCase excludes future measurements relative to evaluation timestamp`() = runTest {
        val evalTime = baseTime + (5 * dayMs)
        (0..10).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 80.0 - (0.1 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTime + (day * dayMs)
                )
            )
        }

        val result = getStatisticalTrendUseCase(evalTime).first()
        assertNotNull(result.estimate)
        assertEquals(79.5, result.estimate!!.currentTrendKg, 0.001)
    }

    @Test
    fun `GetDashboardSummaryUseCase keeps recorded weight primary and trend weight secondary`() = runTest {
        (0..14).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 80.0 - (0.1 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTime + (day * dayMs)
                )
            )
        }

        val summary = getDashboardSummaryUseCase().first()
        // Primary: latest actual entry (78.6 kg)
        assertEquals(78.6, summary.currentWeightInKg!!, 0.001)
        // Secondary: statistical trend weight (78.6 kg)
        assertEquals(78.6, summary.smoothedTrendWeightInKg!!, 0.001)
        // Supporting: weekly change (-0.7 kg/week)
        assertEquals(-0.7, summary.weeklyChangeKg!!, 0.001)
        assertEquals(DataSufficiency.HIGH_CONFIDENCE, summary.dataSufficiency)
        assertEquals(TrendStability.STABLE, summary.trendStability)
    }

    @Test
    fun `GetHistoricalChartDataUseCase filters by time range and calculates historical trend series`() = runTest {
        (0..30).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 85.0 - (0.05 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTime + (day * dayMs)
                )
            )
        }

        val chartData7d = getHistoricalChartDataUseCase(TimeRange.DAYS_7).first()
        assertTrue(chartData7d.size <= 12)
        assertNotNull(chartData7d.first().actualWeightInKg)
        assertNotNull(chartData7d.first().smoothedTrendInKg)
    }
}
