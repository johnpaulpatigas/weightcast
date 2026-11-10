package com.johnpaulpatigas.weightcast.domain.forecast.model

data class WeightForecast(
    val horizon: ForecastHorizon,
    val targetTimestamp: Long,
    val predictedWeightKg: Double?,
    val predictionInterval: PredictionInterval?,
    val availability: ForecastAvailability
) {
    val isAvailable: Boolean
        get() = availability.isAvailable && predictedWeightKg != null

    init {
        if (predictedWeightKg != null) {
            require(!predictedWeightKg.isNaN() && !predictedWeightKg.isInfinite() && predictedWeightKg >= 0.0) {
                "predictedWeightKg must be a non-negative finite number, got $predictedWeightKg"
            }
        }
        if (predictionInterval != null && predictedWeightKg != null) {
            require(predictionInterval.lowerBoundKg <= predictedWeightKg && predictedWeightKg <= predictionInterval.upperBoundKg) {
                "predictedWeightKg ($predictedWeightKg) must lie between lowerBoundKg (${predictionInterval.lowerBoundKg}) and upperBoundKg (${predictionInterval.upperBoundKg})"
            }
        }
    }
}
