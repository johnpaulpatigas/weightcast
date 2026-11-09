package com.johnpaulpatigas.weightcast.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a user weight target/goal.
 */
@Entity(tableName = "weight_goals")
data class WeightGoalEntity(
    @PrimaryKey
    val id: Long = 1,
    val targetWeightInKg: Double,
    val unit: String,
    val goalType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
