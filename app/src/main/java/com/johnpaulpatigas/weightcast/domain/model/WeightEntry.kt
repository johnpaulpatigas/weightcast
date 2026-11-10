package com.johnpaulpatigas.weightcast.domain.model

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter

/**
 * Domain representation of a body weight measurement.
 */
data class WeightEntry(
    val id: Long = 0,
    val weightInKg: Double,
    val originalUnit: WeightUnit,
    val timestamp: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getWeightInUnit(targetUnit: WeightUnit): Double {
        return WeightConverter.fromCanonicalKg(weightInKg, targetUnit)
    }

    fun getFormattedWeight(targetUnit: WeightUnit, decimalPlaces: Int = 2): String {
        return WeightConverter.formatDisplay(weightInKg, targetUnit, decimalPlaces)
    }
}
