package com.johnpaulpatigas.weightcast.domain.forecast.model

enum class ForecastHorizon(val days: Int, val displayName: String) {
    DAYS_7(7, "7 Days"),
    DAYS_30(30, "30 Days"),
    DAYS_90(90, "90 Days")
}
