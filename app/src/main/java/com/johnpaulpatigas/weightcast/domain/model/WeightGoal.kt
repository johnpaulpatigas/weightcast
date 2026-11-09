package com.johnpaulpatigas.weightcast.domain.model

import com.johnpaulpatigas.weightcast.core.model.WeightUnit

enum class GoalType(val displayName: String) {
    WEIGHT_LOSS("Weight Loss"),
    WEIGHT_GAIN("Weight Gain"),
    MAINTENANCE("Maintenance");

    companion object {
        fun fromString(str: String?): GoalType {
            if (str.isNullOrBlank()) return WEIGHT_LOSS
            return entries.find { it.name.equals(str, ignoreCase = true) } ?: WEIGHT_LOSS
        }
    }
}

/**
 * Domain representation of a user weight target goal.
 */
data class WeightGoal(
    val id: Long = 1,
    val targetWeightInKg: Double,
    val unit: WeightUnit,
    val goalType: GoalType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
