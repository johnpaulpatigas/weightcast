package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.forecast.engine.ForecastingEngine
import com.johnpaulpatigas.weightcast.domain.forecast.engine.TargetAchievementEstimator
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastResult
import com.johnpaulpatigas.weightcast.domain.forecast.model.TargetAchievementEstimate
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GenerateWeightForecastUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val getStatisticalTrendUseCase: GetStatisticalTrendUseCase
) {
    operator fun invoke(evaluationTimestamp: Long = System.currentTimeMillis()): Flow<Pair<ForecastResult, TargetAchievementEstimate>> {
        return combine(
            weightRepository.getAllEntries(),
            goalRepository.getGoal()
        ) { rawEntries, goal ->
            invoke(rawEntries, goal, evaluationTimestamp, bootstrapIterations = 50)
        }
    }

    operator fun invoke(
        rawEntries: List<WeightEntry>,
        goal: WeightGoal?,
        evaluationTimestamp: Long = System.currentTimeMillis(),
        horizons: List<ForecastHorizon> = ForecastHorizon.entries,
        bootstrapIterations: Int = 500,
        randomSeed: Long? = null
    ): Pair<ForecastResult, TargetAchievementEstimate> {
        val trendResult = getStatisticalTrendUseCase(
            rawEntries = rawEntries,
            evaluationTimestamp = evaluationTimestamp
        )

        val prepared = DataPreparation.prepare(rawEntries)
        val validInWindow = prepared.measurements.filter { it.timestamp <= evaluationTimestamp }

        val forecastResult = ForecastingEngine.generateForecasts(
            preparedMeasurements = validInWindow,
            estimate = trendResult.estimate,
            dataSufficiency = trendResult.sufficiency,
            trendStability = trendResult.estimate?.trendStability ?: TrendStability.UNSTABLE,
            evaluationTimestamp = evaluationTimestamp,
            horizons = horizons,
            bootstrapIterations = bootstrapIterations,
            randomSeed = randomSeed
        )

        val targetEstimate = TargetAchievementEstimator.estimateTargetAchievement(
            targetWeightKg = goal?.targetWeightInKg,
            preparedMeasurements = validInWindow,
            estimate = trendResult.estimate,
            evaluationTimestamp = evaluationTimestamp,
            overallAvailability = forecastResult.overallAvailability,
            bootstrapIterations = bootstrapIterations,
            randomSeed = randomSeed
        )

        return Pair(forecastResult, targetEstimate)
    }
}
