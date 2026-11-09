package com.johnpaulpatigas.weightcast.domain.trend.model

/**
 * Represents the statistical sufficiency and confidence of data used for weight trend estimation.
 */
enum class DataSufficiency(val displayName: String, val description: String) {
    INSUFFICIENT_DATA(
        displayName = "Insufficient Data",
        description = "Not enough weight measurements to estimate a defensible trend."
    ),
    LOW_CONFIDENCE(
        displayName = "Low Confidence",
        description = "Minimal data available; trend is subject to high uncertainty."
    ),
    MODERATE_CONFIDENCE(
        displayName = "Moderate Confidence",
        description = "Adequate measurement density and coverage across recent days."
    ),
    HIGH_CONFIDENCE(
        displayName = "High Confidence",
        description = "Strong measurement history with high density and multi-week coverage."
    )
}
