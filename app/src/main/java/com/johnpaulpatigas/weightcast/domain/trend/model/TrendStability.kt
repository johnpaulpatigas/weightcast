package com.johnpaulpatigas.weightcast.domain.trend.model

/**
 * Represents the temporal stability and consistency of the estimated weight trend.
 */
enum class TrendStability(val displayName: String, val description: String) {
    UNSTABLE(
        displayName = "Unstable",
        description = "High residual variability or recent trajectory fluctuations detected."
    ),
    MODERATE(
        displayName = "Moderate",
        description = "Moderate residual variance around the estimated trend line."
    ),
    STABLE(
        displayName = "Stable",
        description = "Consistently steady weight trajectory with minimal residual noise."
    )
}
