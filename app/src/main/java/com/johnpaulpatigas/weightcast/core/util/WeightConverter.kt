package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utility for formatting weight measurements in canonical Kilograms (KG).
 */
object WeightConverter {

    /**
     * Converts a weight value from one unit to another (exclusively KG).
     */
    fun convert(value: Double, fromUnit: WeightUnit = WeightUnit.KG, toUnit: WeightUnit = WeightUnit.KG): Double {
        return value
    }

    /**
     * Converts a weight into canonical Kilograms (KG).
     */
    fun toCanonicalKg(value: Double, unit: WeightUnit = WeightUnit.KG): Double {
        return value
    }

    /**
     * Converts canonical Kilograms into target unit.
     */
    fun fromCanonicalKg(kgValue: Double, targetUnit: WeightUnit = WeightUnit.KG): Double {
        return kgValue
    }

    /**
     * Rounds a double value to specified decimal places.
     */
    fun roundTo(value: Double, decimalPlaces: Int = 1): Double {
        require(decimalPlaces >= 0) { "Decimal places must be non-negative" }
        val factor = 10.0.pow(decimalPlaces)
        return (value * factor).roundToInt() / factor
    }

    /**
     * Formats a canonical KG weight value for display with unit symbol.
     */
    fun formatDisplay(
        canonicalKgValue: Double,
        targetUnit: WeightUnit = WeightUnit.KG,
        decimalPlaces: Int = 1
    ): String {
        return canonicalKgValue.formatKg(decimalPlaces)
    }
}

/**
 * Top-level Kotlin extension for formatting Double weight in kilograms (kg).
 */
fun Double.formatKg(decimals: Int = 1): String = "%.${decimals}f kg".format(this)
