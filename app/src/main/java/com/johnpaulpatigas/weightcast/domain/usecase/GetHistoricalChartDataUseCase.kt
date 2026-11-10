package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.di.DefaultDispatcher
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.IncrementalTheilSenEvaluator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHistoricalChartDataUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val getStatisticalTrendUseCase: GetStatisticalTrendUseCase,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    operator fun invoke(timeRange: TimeRange): Flow<List<ChartPoint>> {
        return weightRepository.getAllEntries().map { allEntries ->
            if (allEntries.isEmpty()) return@map emptyList()

            val sortedAsc = allEntries.sortedBy { it.timestamp }
            val latestTimestamp = sortedAsc.last().timestamp

            val filteredEntries = TrendCalculator.filterEntriesByTimeRange(
                entries = sortedAsc,
                range = timeRange,
                nowMillis = latestTimestamp
            )

            IncrementalTheilSenEvaluator.evaluateHistoricalChartPoints(
                allEntries = sortedAsc,
                targetEntries = filteredEntries
            )
        }.flowOn(defaultDispatcher)
    }
}
