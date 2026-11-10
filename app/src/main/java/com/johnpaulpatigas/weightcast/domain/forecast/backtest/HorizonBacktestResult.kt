package com.johnpaulpatigas.weightcast.domain.forecast.backtest

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon

data class HorizonBacktestResult(
    val horizon: ForecastHorizon,
    val totalEvaluatedOrigins: Int,
    val validForecastsCount: Int,
    val matchedObservationsCount: Int,
    val meanAbsoluteErrorKg: Double,
    val rootMeanSquaredErrorKg: Double,
    val meanAbsolutePercentageError: Double?,
    val empiricalCoverageRate: Double,
    val meanIntervalWidthKg: Double
)
