package com.johnpaulpatigas.weightcast.domain.forecast.model

data class PredictionInterval(
    val lowerBoundKg: Double,
    val upperBoundKg: Double,
    val confidenceLevel: Double = 0.95
) {
    val intervalWidthKg: Double
        get() = upperBoundKg - lowerBoundKg

    init {
        require(!lowerBoundKg.isNaN() && !lowerBoundKg.isInfinite() && lowerBoundKg >= 0.0) {
            "lowerBoundKg must be a non-negative finite number, got $lowerBoundKg"
        }
        require(!upperBoundKg.isNaN() && !upperBoundKg.isInfinite() && upperBoundKg >= 0.0) {
            "upperBoundKg must be a non-negative finite number, got $upperBoundKg"
        }
        require(lowerBoundKg <= upperBoundKg) {
            "lowerBoundKg ($lowerBoundKg) cannot be greater than upperBoundKg ($upperBoundKg)"
        }
        require(!confidenceLevel.isNaN() && confidenceLevel in 0.0..1.0) {
            "confidenceLevel must be between 0.0 and 1.0, got $confidenceLevel"
        }
    }
}
