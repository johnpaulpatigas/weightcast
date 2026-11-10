package com.johnpaulpatigas.weightcast.domain.forecast.model

data class ForecastAvailability(
    val status: ForecastAvailabilityStatus,
    val reason: String
) {
    val isAvailable: Boolean
        get() = status == ForecastAvailabilityStatus.AVAILABLE
}
