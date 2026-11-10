package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.StatisticalTrendEvaluator
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendEstimationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStatisticalTrendUseCase @Inject constructor(
    private val weightRepository: WeightRepository
) {
    operator fun invoke(evaluationTimestamp: Long = System.currentTimeMillis()): Flow<TrendEstimationResult> {
        return weightRepository.getAllEntries().map { entries ->
            StatisticalTrendEvaluator.evaluateTrend(
                rawEntries = entries,
                evaluationTimestamp = evaluationTimestamp
            )
        }
    }
}
