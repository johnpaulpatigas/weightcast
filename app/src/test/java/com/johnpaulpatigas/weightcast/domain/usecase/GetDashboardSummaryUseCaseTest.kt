package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeGoalRepository : GoalRepository {
    private val _goalFlow = MutableStateFlow<WeightGoal?>(null)

    override fun getGoal(): Flow<WeightGoal?> = _goalFlow

    override suspend fun saveGoal(goal: WeightGoal) {
        _goalFlow.value = goal
    }

    override suspend fun clearGoal() {
        _goalFlow.value = null
    }
}

class GetDashboardSummaryUseCaseTest {

    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var getStatisticalTrendUseCase: GetStatisticalTrendUseCase
    private lateinit var generateWeightForecastUseCase: GenerateWeightForecastUseCase
    private lateinit var useCase: GetDashboardSummaryUseCase

    @Before
    fun setUp() {
        weightRepository = FakeWeightRepository()
        goalRepository = FakeGoalRepository()
        getStatisticalTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        generateWeightForecastUseCase = GenerateWeightForecastUseCase(weightRepository, goalRepository, getStatisticalTrendUseCase)
        useCase = GetDashboardSummaryUseCase(weightRepository, goalRepository, getStatisticalTrendUseCase, generateWeightForecastUseCase)
    }

    @Test
    fun `invoke with no entries returns empty summary`() = runTest {
        val summary = useCase().first()
        assertNull(summary.currentWeightInKg)
        assertNull(summary.smoothedTrendWeightInKg)
        assertNull(summary.weeklyChangeKg)
        assertTrue(summary.recentEntries.isEmpty())
    }

    @Test
    fun `invoke with entries calculates summary metrics correctly`() = runTest {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        weightRepository.addEntry(WeightEntry(weightInKg = 80.0, originalUnit = WeightUnit.KG, timestamp = now - (8 * dayMs)))
        weightRepository.addEntry(WeightEntry(weightInKg = 78.0, originalUnit = WeightUnit.KG, timestamp = now))

        val summary = useCase().first()
        assertNotNull(summary.currentWeightInKg)
        assertEquals(78.0, summary.currentWeightInKg!!, 0.001)
        assertEquals(2, summary.recentEntries.size)
    }
}

private fun assertTrue(condition: Boolean) {
    org.junit.Assert.assertTrue(condition)
}
