package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TemporalIntegrityTest {

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
    fun `no future-data leakage relative to historical evaluation timestamps`() = runTest {
        // Add 10 past entries and 5 future entries relative to evalTime (day 7)
        (0..14).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 80.0 - (0.2 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTime + (day * dayMs)
                )
            )
        }

        val evalTimeDay7 = baseTime + (7 * dayMs)
        val trendAtDay7 = getStatisticalTrendUseCase(evalTimeDay7).first()

        // With evalTime at Day 7, weight is 80.0 - 0.2*7 = 78.6 kg.
        assertNotNull(trendAtDay7.estimate)
        assertEquals(78.6, trendAtDay7.estimate!!.currentTrendKg, 0.001)

        // Adding extreme future points (e.g. 200kg on day 20) should have ZERO impact on Day 7 trend
        weightRepository.addEntry(
            WeightEntry(
                id = 99L,
                weightInKg = 200.0,
                originalUnit = WeightUnit.KG,
                timestamp = baseTime + (20 * dayMs)
            )
        )

        val trendAtDay7AfterFuture = getStatisticalTrendUseCase(evalTimeDay7).first()
        assertEquals(78.6, trendAtDay7AfterFuture.estimate!!.currentTrendKg, 0.001)
    }

    @Test
    fun `multiple measurements on the same UTC date are aggregated without duplication`() = runTest {
        val sameDayTs = baseTime + (5 * dayMs)
        weightRepository.addEntry(WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = sameDayTs + 1000))
        weightRepository.addEntry(WeightEntry(id = 2, weightInKg = 82.0, originalUnit = WeightUnit.KG, timestamp = sameDayTs + 5000))
        weightRepository.addEntry(WeightEntry(id = 3, weightInKg = 78.0, originalUnit = WeightUnit.KG, timestamp = sameDayTs + 10000))

        // Average of 80.0, 82.0, 78.0 is 80.0
        val trendResult = getStatisticalTrendUseCase(sameDayTs + 20000).first()
        // Single day aggregated measurement is insufficient (< 3 distinct daily representatives)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, trendResult.sufficiency)
        assertNull(trendResult.estimate)
    }

    @Test
    fun `missing dates are handled gracefully without artificial zero padding`() = runTest {
        // Entries with gaps: day 0, day 5, day 12
        weightRepository.addEntry(WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime))
        weightRepository.addEntry(WeightEntry(id = 2, weightInKg = 79.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (5 * dayMs)))
        weightRepository.addEntry(WeightEntry(id = 3, weightInKg = 77.6, originalUnit = WeightUnit.KG, timestamp = baseTime + (12 * dayMs)))

        val trendResult = getStatisticalTrendUseCase(baseTime + (12 * dayMs)).first()
        val estimate = trendResult.estimate
        assertNotNull(estimate)
        // Check slope is correct: (77.6 - 80.0)/12.0 = -0.2 kg/day
        assertEquals(-0.2, estimate?.slopeKgPerDay ?: 0.0, 0.001)
        assertEquals(77.6, estimate?.currentTrendKg ?: 0.0, 0.001)
    }

    @Test
    fun `insufficient historical data yields null trend in historical chart`() = runTest {
        // Only 2 entries
        weightRepository.addEntry(WeightEntry(id = 1, weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = baseTime))
        weightRepository.addEntry(WeightEntry(id = 2, weightInKg = 79.0, originalUnit = WeightUnit.KG, timestamp = baseTime + (1 * dayMs)))

        val chartPoints = getHistoricalChartDataUseCase(TimeRange.ALL_TIME).first()
        // 2 historical points
        assertEquals(2, chartPoints.filter { it.actualWeightInKg != null }.size)
        assertEquals(80.0, chartPoints[0].actualWeightInKg!!, 0.001)
        assertNull(chartPoints[0].smoothedTrendInKg)
        assertEquals(79.0, chartPoints[1].actualWeightInKg!!, 0.001)
        assertNull(chartPoints[1].smoothedTrendInKg)
    }

    @Test
    fun `invalid regression result yields unavailable estimation result`() = runTest {
        // Measurements that produce non-plausible fitted curve
        val trendResult = getStatisticalTrendUseCase(baseTime).first()
        assertNull(trendResult.estimate)
        assertEquals(DataSufficiency.INSUFFICIENT_DATA, trendResult.sufficiency)
    }
}
