package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHistoricalChartDataUseCase @Inject constructor(
    private val weightRepository: WeightRepository
) {
    operator fun invoke(timeRange: TimeRange): Flow<List<ChartPoint>> {
        return weightRepository.getAllEntries().map { allEntries ->
            if (allEntries.isEmpty()) return@map emptyList()

            val sortedAsc = allEntries.sortedBy { it.timestamp }
            val trendList = TrendCalculator.calculate7DayMovingAverage(sortedAsc)
            val trendMap = trendList.toMap()

            val filteredEntries = TrendCalculator.filterEntriesByTimeRange(sortedAsc, timeRange)

            filteredEntries.map { entry ->
                ChartPoint(
                    timestamp = entry.timestamp,
                    actualWeightInKg = entry.weightInKg,
                    smoothedTrendInKg = trendMap[entry.timestamp] ?: entry.weightInKg
                )
            }
        }
    }
}
