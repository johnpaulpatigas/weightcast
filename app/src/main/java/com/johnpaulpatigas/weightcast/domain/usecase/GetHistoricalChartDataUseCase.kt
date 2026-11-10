package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.StatisticalTrendEvaluator
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
            val maTrendMap = TrendCalculator.calculate7DayMovingAverage(sortedAsc).toMap()
            val latestTimestamp = sortedAsc.last().timestamp

            val filteredEntries = TrendCalculator.filterEntriesByTimeRange(
                entries = sortedAsc,
                range = timeRange,
                nowMillis = latestTimestamp
            )

            filteredEntries.map { entry ->
                val trendResult = StatisticalTrendEvaluator.evaluateTrend(
                    rawEntries = sortedAsc.filter { it.timestamp <= entry.timestamp },
                    evaluationTimestamp = entry.timestamp
                )

                val robustTrendKg = trendResult.estimate?.currentTrendKg
                val trendKg = robustTrendKg ?: maTrendMap[entry.timestamp] ?: entry.weightInKg

                ChartPoint(
                    timestamp = entry.timestamp,
                    actualWeightInKg = entry.weightInKg,
                    smoothedTrendInKg = trendKg
                )
            }
        }
    }
}
