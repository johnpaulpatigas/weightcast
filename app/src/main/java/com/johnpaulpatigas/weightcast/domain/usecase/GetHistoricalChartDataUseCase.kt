package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.di.DefaultDispatcher
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.IncrementalTheilSenEvaluator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHistoricalChartDataUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val generateWeightForecastUseCase: GenerateWeightForecastUseCase,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    operator fun invoke(timeRange: TimeRange, includeForecasts: Boolean = true): Flow<List<ChartPoint>> {
        if (!includeForecasts) {
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

        return combine(
            weightRepository.getAllEntries(),
            generateWeightForecastUseCase()
        ) { allEntries, (forecastResult, _) ->
            if (allEntries.isEmpty()) return@combine emptyList()

            val sortedAsc = allEntries.sortedBy { it.timestamp }
            val latestTimestamp = sortedAsc.last().timestamp

            val filteredEntries = TrendCalculator.filterEntriesByTimeRange(
                entries = sortedAsc,
                range = timeRange,
                nowMillis = latestTimestamp
            )

            val historicalPoints = IncrementalTheilSenEvaluator.evaluateHistoricalChartPoints(
                allEntries = sortedAsc,
                targetEntries = filteredEntries
            )

            val forecastPoints = mutableListOf<ChartPoint>()
            if (forecastResult.isAnyForecastAvailable) {
                val originTrend = historicalPoints.lastOrNull()?.smoothedTrendInKg ?: sortedAsc.last().weightInKg

                forecastPoints.add(
                    ChartPoint(
                        timestamp = latestTimestamp,
                        actualWeightInKg = null,
                        smoothedTrendInKg = null,
                        forecastWeightInKg = originTrend,
                        lowerBoundInKg = originTrend,
                        upperBoundInKg = originTrend
                    )
                )

                for (horizon in ForecastHorizon.entries) {
                    val wf = forecastResult.forecasts[horizon]
                    if (wf != null && wf.isAvailable && wf.predictedWeightKg != null) {
                        forecastPoints.add(
                            ChartPoint(
                                timestamp = wf.targetTimestamp,
                                actualWeightInKg = null,
                                smoothedTrendInKg = null,
                                forecastWeightInKg = wf.predictedWeightKg,
                                lowerBoundInKg = wf.predictionInterval?.lowerBoundKg ?: wf.predictedWeightKg,
                                upperBoundInKg = wf.predictionInterval?.upperBoundKg ?: wf.predictedWeightKg
                            )
                        )
                    }
                }
            }

            historicalPoints + forecastPoints
        }.flowOn(defaultDispatcher)
    }
}
