package com.johnpaulpatigas.weightcast.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a single body weight measurement.
 *
 * All weight values are stored internally in canonical Kilograms [weightInKg].
 */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["timestamp"])]
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightInKg: Double,
    val unit: String,
    val timestamp: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
