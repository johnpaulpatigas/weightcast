package com.johnpaulpatigas.weightcast.domain.forecast.model

import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability

data class ForecastResult(
    val forecastOriginTimestamp: Long,
    val forecasts: Map<ForecastHorizon, WeightForecast>,
    val dataSufficiency: DataSufficiency,
    val trendStability: TrendStability,
    val overallAvailability: ForecastAvailability
) {
    val isAnyForecastAvailable: Boolean
        get() = forecasts.values.any { it.isAvailable }
}
