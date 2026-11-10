package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHistoricalChartDataUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val getStatisticalTrendUseCase: GetStatisticalTrendUseCase
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

            filteredEntries.map { entry ->
                val trendResult = getStatisticalTrendUseCase(
                    rawEntries = sortedAsc.filter { it.timestamp <= entry.timestamp },
                    evaluationTimestamp = entry.timestamp
                )

                ChartPoint(
                    timestamp = entry.timestamp,
                    actualWeightInKg = entry.weightInKg,
                    smoothedTrendInKg = trendResult.estimate?.currentTrendKg
                )
            }
        }
    }
}
