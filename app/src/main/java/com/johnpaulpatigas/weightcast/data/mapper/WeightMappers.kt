package com.johnpaulpatigas.weightcast.data.mapper

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.data.local.entity.WeightEntryEntity
import com.johnpaulpatigas.weightcast.data.local.entity.WeightGoalEntity
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal

fun WeightEntryEntity.toDomain(): WeightEntry {
    return WeightEntry(
        id = id,
        weightInKg = weightInKg,
        originalUnit = WeightUnit.fromString(unit),
        timestamp = timestamp,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WeightEntry.toEntity(): WeightEntryEntity {
    return WeightEntryEntity(
        id = id,
        weightInKg = weightInKg,
        unit = originalUnit.name,
        timestamp = timestamp,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WeightGoalEntity.toDomain(): WeightGoal {
    return WeightGoal(
        id = id,
        targetWeightInKg = targetWeightInKg,
        unit = WeightUnit.fromString(unit),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WeightGoal.toEntity(): WeightGoalEntity {
    return WeightGoalEntity(
        id = id,
        targetWeightInKg = targetWeightInKg,
        unit = unit.name,
        goalType = "TARGET",
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
