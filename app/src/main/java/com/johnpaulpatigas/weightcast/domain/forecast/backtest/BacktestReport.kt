package com.johnpaulpatigas.weightcast.domain.forecast.backtest

import com.johnpaulpatigas.weightcast.domain.forecast.model.ForecastHorizon

data class BacktestReport(
    val totalHistoricalOriginsCount: Int,
    val excludedOriginsCount: Int,
    val horizonResults: Map<ForecastHorizon, HorizonBacktestResult>
)
