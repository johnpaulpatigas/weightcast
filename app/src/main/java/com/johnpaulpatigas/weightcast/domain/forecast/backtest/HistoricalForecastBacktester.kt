package com.johnpaulpatigas.weightcast.domain.forecast.backtest

import com.johnpaulpatigas.weightcast.domain.forecast.engine.ForecastingEngine
import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataPreparation
import com.johnpaulpatigas.weightcast.domain.trend.engine.DataSufficiencyEvaluator
import com.johnpaulpatigas.weightcast.domain.trend.engine.TheilSenEstimator
import com.johnpaulpatigas.weightcast.domain.trend.engine.TrendStabilityEvaluator
import kotlin.math.abs
import kotlin.math.sqrt

object HistoricalForecastBacktester {

    private const val TWO_DAYS_MILLIS = 2 * DataPreparation.MILLIS_PER_DAY

    /**
     * Evaluates historical forecast performance and empirical prediction interval coverage
     * using rolling forecast origins over raw historical entries.
     */
    fun runBacktest(
        rawEntries: List<WeightEntry>,
        minTrainingMeasurements: Int = 5,
        horizons: List<ForecastHorizon> = ForecastHorizon.entries,
        bootstrapIterations: Int = 200,
        randomSeed: Long? = 42L
    ): BacktestReport {
        val prepared = DataPreparation.prepare(rawEntries)
        val allMeasurements = prepared.measurements

        if (allMeasurements.size < minTrainingMeasurements) {
            val emptyMap = horizons.associateWith { horizon ->
                HorizonBacktestResult(
                    horizon = horizon,
                    totalEvaluatedOrigins = 0,
                    validForecastsCount = 0,
                    meanAbsoluteErrorKg = 0.0,
                    rootMeanSquaredErrorKg = 0.0,
                    meanAbsolutePercentageError = null,
                    empiricalCoverageRate = 0.0,
                    meanIntervalWidthKg = 0.0
                )
            }
            return BacktestReport(
                totalHistoricalOriginsCount = 0,
                excludedOriginsCount = 0,
                horizonResults = emptyMap
            )
        }

        val absErrors = horizons.associateWith { mutableListOf<Double>() }
        val sqErrors = horizons.associateWith { mutableListOf<Double>() }
        val pctErrors = horizons.associateWith { mutableListOf<Double>() }
        val coverageList = horizons.associateWith { mutableListOf<Boolean>() }
        val intervalWidths = horizons.associateWith { mutableListOf<Double>() }

        val originCount = allMeasurements.size - minTrainingMeasurements
        var excludedOriginsCount = 0

        for (k in minTrainingMeasurements - 1 until allMeasurements.size - 1) {
            val originTs = allMeasurements[k].timestamp
            val historicalSubset = allMeasurements.take(k + 1)

            val sufficiency = DataSufficiencyEvaluator.evaluate(historicalSubset, originTs)
            val estimate = TheilSenEstimator.estimate(
                preparedMeasurements = historicalSubset,
                evaluationTimestamp = originTs,
                dataSufficiency = sufficiency
            )

            val (stability, _) = TrendStabilityEvaluator.evaluate(historicalSubset, estimate)

            val forecastResult = ForecastingEngine.generateForecasts(
                preparedMeasurements = historicalSubset,
                estimate = estimate,
                dataSufficiency = sufficiency,
                trendStability = stability,
                evaluationTimestamp = originTs,
                horizons = horizons,
                bootstrapIterations = bootstrapIterations,
                randomSeed = randomSeed
            )

            if (!forecastResult.isAnyForecastAvailable) {
                excludedOriginsCount++
            }

            for (horizon in horizons) {
                val forecast = forecastResult.forecasts[horizon]
                if (forecast != null && forecast.isAvailable && forecast.predictedWeightKg != null) {
                    val targetTs = forecast.targetTimestamp
                    val actualObs = allMeasurements
                        .filter { it.timestamp > originTs }
                        .minByOrNull { abs(it.timestamp - targetTs) }

                    if (actualObs != null && abs(actualObs.timestamp - targetTs) <= TWO_DAYS_MILLIS) {
                        val actualKg = actualObs.weightKg
                        val predictedKg = forecast.predictedWeightKg
                        val error = actualKg - predictedKg

                        absErrors[horizon]?.add(abs(error))
                        sqErrors[horizon]?.add(error * error)
                        if (actualKg > 0.0) {
                            pctErrors[horizon]?.add((abs(error) / actualKg) * 100.0)
                        }

                        val pi = forecast.predictionInterval
                        if (pi != null) {
                            val isCovered = actualKg >= pi.lowerBoundKg && actualKg <= pi.upperBoundKg
                            coverageList[horizon]?.add(isCovered)
                            intervalWidths[horizon]?.add(pi.intervalWidthKg)
                        }
                    }
                }
            }
        }

        val horizonMap = horizons.associateWith { horizon ->
            val absErrList = absErrors[horizon] ?: emptyList()
            val sqErrList = sqErrors[horizon] ?: emptyList()
            val pctErrList = pctErrors[horizon] ?: emptyList()
            val covList = coverageList[horizon] ?: emptyList()
            val widthList = intervalWidths[horizon] ?: emptyList()

            val nValid = absErrList.size

            val mae = if (nValid > 0) absErrList.average() else 0.0
            val rmse = if (nValid > 0) sqrt(sqErrList.average()) else 0.0
            val mape = if (pctErrList.isNotEmpty()) pctErrList.average() else null
            val coverage = if (covList.isNotEmpty()) covList.count { it }.toDouble() / covList.size.toDouble() else 0.0
            val meanWidth = if (widthList.isNotEmpty()) widthList.average() else 0.0

            HorizonBacktestResult(
                horizon = horizon,
                totalEvaluatedOrigins = originCount,
                validForecastsCount = nValid,
                meanAbsoluteErrorKg = mae,
                rootMeanSquaredErrorKg = rmse,
                meanAbsolutePercentageError = mape,
                empiricalCoverageRate = coverage,
                meanIntervalWidthKg = meanWidth
            )
        }

        return BacktestReport(
            totalHistoricalOriginsCount = originCount,
            excludedOriginsCount = excludedOriginsCount,
            horizonResults = horizonMap
        )
    }
}
