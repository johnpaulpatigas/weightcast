package com.johnpaulpatigas.weightcast.domain.forecast.model

enum class ForecastAvailabilityStatus {
    AVAILABLE,
    INSUFFICIENT_DATA,
    LOW_CONFIDENCE,
    UNSTABLE_TREND,
    INVALID_SLOPE,
    PHYSICAL_CONSTRAINT_VIOLATION
}
