package com.johnpaulpatigas.weightcast.core.model

/**
 * Supported weight unit in WeightCast.
 *
 * WeightCast exclusively uses Kilograms (KG) for all weight measurements.
 */
enum class WeightUnit(
    val symbol: String,
    val displayName: String,
    val lbsPerKgRatio: Double
) {
    KG("kg", "Kilograms", 1.0);

    companion object {
        fun fromString(unitStr: String?): WeightUnit {
            return KG
        }
    }
}
