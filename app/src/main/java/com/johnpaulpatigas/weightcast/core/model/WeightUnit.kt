package com.johnpaulpatigas.weightcast.core.model

/**
 * Supported weight units in WeightCast.
 *
 * All weight measurements are stored internally in canonical Kilograms (KG).
 */
enum class WeightUnit(
    val symbol: String,
    val displayName: String,
    val lbsPerKgRatio: Double
) {
    KG("kg", "Kilograms", 1.0),
    LBS("lbs", "Pounds", 2.20462262185);

    companion object {
        const val LBS_PER_KG = 2.20462262185

        fun fromString(unitStr: String?): WeightUnit {
            if (unitStr.isNull_or_blank()) return KG
            return entries.find {
                it.name.equals(unitStr, ignoreCase = true) || it.symbol.equals(unitStr, ignoreCase = true)
            } ?: KG
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
