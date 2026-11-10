package com.johnpaulpatigas.weightcast.domain.forecast.model

data class TargetAchievementEstimate(
    val targetWeightKg: Double,
    val currentTrendWeightKg: Double?,
    val estimatedAchievementTimestamp: Long?,
    val estimatedDaysToTarget: Double?,
    val lowerBoundDaysToTarget: Double?,
    val upperBoundDaysToTarget: Double?,
    val isAchievable: Boolean,
    val isAlreadyAchieved: Boolean,
    val reachingRatio: Double? = null,
    val availabilityReason: String
) {
    init {
        require(!targetWeightKg.isNaN() && !targetWeightKg.isInfinite() && targetWeightKg >= 0.0) {
            "targetWeightKg must be a non-negative finite number, got $targetWeightKg"
        }
        if (reachingRatio != null) {
            require(!reachingRatio.isNaN() && reachingRatio in 0.0..1.0) {
                "reachingRatio must be between 0.0 and 1.0, got $reachingRatio"
            }
        }
    }
}
