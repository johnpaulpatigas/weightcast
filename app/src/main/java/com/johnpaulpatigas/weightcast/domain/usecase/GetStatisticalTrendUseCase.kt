package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.StatisticalTrendEvaluator
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendEstimationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStatisticalTrendUseCase @Inject constructor(
    private val weightRepository: WeightRepository
) {
    /**
     * Evaluates statistical trend from the weight repository as a Flow at [evaluationTimestamp].
     */
    operator fun invoke(evaluationTimestamp: Long = System.currentTimeMillis()): Flow<TrendEstimationResult> {
        return weightRepository.getAllEntries().map { entries ->
            invoke(entries, evaluationTimestamp)
        }
    }

    /**
     * Evaluates statistical trend directly from a given list of raw entries at [evaluationTimestamp].
     */
    operator fun invoke(rawEntries: List<WeightEntry>, evaluationTimestamp: Long): TrendEstimationResult {
        return StatisticalTrendEvaluator.evaluateTrend(
            rawEntries = rawEntries,
            evaluationTimestamp = evaluationTimestamp
        )
    }
}
