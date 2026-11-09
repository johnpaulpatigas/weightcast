package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utility for converting and formatting weight measurements.
 */
object WeightConverter {

    fun kgToLbs(kg: Double): Double {
        return kg * WeightUnit.LBS_PER_KG
    }

    fun lbsToKg(lbs: Double): Double {
        return lbs / WeightUnit.LBS_PER_KG
    }

    /**
     * Converts a weight value from one unit to another.
     */
    fun convert(value: Double, fromUnit: WeightUnit, toUnit: WeightUnit): Double {
        if (fromUnit == toUnit) return value
        return when (fromUnit) {
            WeightUnit.KG -> kgToLbs(value)
            WeightUnit.LBS -> lbsToKg(value)
        }
    }

    /**
     * Converts a weight given in any unit into canonical Kilograms (KG).
     */
    fun toCanonicalKg(value: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> value
            WeightUnit.LBS -> lbsToKg(value)
        }
    }

    /**
     * Converts canonical Kilograms into target unit.
     */
    fun fromCanonicalKg(kgValue: Double, targetUnit: WeightUnit): Double {
        return when (targetUnit) {
            WeightUnit.KG -> kgValue
            WeightUnit.LBS -> kgToLbs(kgValue)
        }
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
        targetUnit: WeightUnit,
        decimalPlaces: Int = 1
    ): String {
        val converted = fromCanonicalKg(canonicalKgValue, targetUnit)
        val rounded = roundTo(converted, decimalPlaces)
        return "$rounded ${targetUnit.symbol}"
    }
}
