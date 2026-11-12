package com.johnpaulpatigas.weightcast.feature.dashboard

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.usecase.FakeGoalRepository
import com.johnpaulpatigas.weightcast.domain.usecase.FakeWeightRepository
import com.johnpaulpatigas.weightcast.domain.usecase.GenerateWeightForecastUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetDashboardSummaryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetHistoricalChartDataUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetStatisticalTrendUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        weightRepository = FakeWeightRepository()
        goalRepository = FakeGoalRepository()

        val getTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        val generateForecastUseCase = GenerateWeightForecastUseCase(weightRepository, goalRepository, getTrendUseCase)
        val getSummaryUseCase = GetDashboardSummaryUseCase(weightRepository, goalRepository, getTrendUseCase, generateForecastUseCase)
        val getChartDataUseCase = GetHistoricalChartDataUseCase(weightRepository, generateForecastUseCase, testDispatcher)

        viewModel = DashboardViewModel(
            getDashboardSummaryUseCase = getSummaryUseCase,
            getHistoricalChartDataUseCase = getChartDataUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state loads summary and default time range`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TimeRange.MONTHS_1, state.selectedTimeRange)
    }

    @Test
    fun `onTimeRangeSelected updates selectedTimeRange`() = runTest {
        viewModel.onTimeRangeSelected(TimeRange.MONTHS_3)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TimeRange.MONTHS_3, viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `when weight entries exist, chart points and summary update`() = runTest {
        val now = System.currentTimeMillis()
        weightRepository.addEntry(WeightEntry(weightInKg = 75.0, originalUnit = WeightUnit.KG, timestamp = now))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(75.0, state.summary.currentWeightInKg!!, 0.001)
        assertEquals(1, state.chartPoints.size)
    }
}
