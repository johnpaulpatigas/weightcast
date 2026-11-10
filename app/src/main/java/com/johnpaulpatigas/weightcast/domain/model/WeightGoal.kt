package com.johnpaulpatigas.weightcast.domain.model

import com.johnpaulpatigas.weightcast.core.model.WeightUnit

/**
 * Domain representation of a user weight target goal.
 */
data class WeightGoal(
    val id: Long = 1,
    val targetWeightInKg: Double,
    val unit: WeightUnit = WeightUnit.KG,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
