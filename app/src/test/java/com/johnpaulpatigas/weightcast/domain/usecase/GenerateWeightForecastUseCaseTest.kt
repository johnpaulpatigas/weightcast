package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateWeightForecastUseCaseTest {

    private lateinit var weightRepository: FakeWeightRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var getStatisticalTrendUseCase: GetStatisticalTrendUseCase
    private lateinit var useCase: GenerateWeightForecastUseCase

    private val dayMs = 86_400_000L
    private val baseTs = 100 * dayMs

    @Before
    fun setUp() {
        weightRepository = FakeWeightRepository()
        goalRepository = FakeGoalRepository()
        getStatisticalTrendUseCase = GetStatisticalTrendUseCase(weightRepository)
        useCase = GenerateWeightForecastUseCase(
            weightRepository = weightRepository,
            goalRepository = goalRepository,
            getStatisticalTrendUseCase = getStatisticalTrendUseCase
        )
    }

    @Test
    fun `invoke with high confidence dataset generates valid forecast and target estimates`() = runTest {
        (0 until 14).forEach { day ->
            weightRepository.addEntry(
                WeightEntry(
                    id = (day + 1).toLong(),
                    weightInKg = 80.0 - (0.1 * day),
                    originalUnit = WeightUnit.KG,
                    timestamp = baseTs + (day * dayMs)
                )
            )
        }
        goalRepository.saveGoal(WeightGoal(targetWeightInKg = 75.0))

        val (forecastResult, targetEstimate) = useCase(evaluationTimestamp = baseTs + (13 * dayMs)).first()

        assertTrue(forecastResult.isAnyForecastAvailable)

        val f7 = forecastResult.forecasts[ForecastHorizon.DAYS_7]
        assertNotNull(f7)
        assertTrue(f7!!.isAvailable)
        assertEquals(78.0, f7.predictedWeightKg!!, 0.1)

        assertTrue(targetEstimate.isAchievable)
        assertNotNull(targetEstimate.estimatedDaysToTarget)
    }
}
