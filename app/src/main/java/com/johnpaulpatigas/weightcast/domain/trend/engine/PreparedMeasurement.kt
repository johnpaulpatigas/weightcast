package com.johnpaulpatigas.weightcast.domain.trend.engine

/**
 * Cleaned, normalized, and daily-aggregated weight measurement point prepared for trend estimation.
 *
 * @property timestamp Representative epoch timestamp in milliseconds (UTC midnight of measurement day).
 * @property elapsedDays Elapsed time in fractional days relative to dataset baseline t0.
 * @property weightKg Canonical representative weight in Kilograms (KG).
 * @property originalMeasurementCount Number of raw weight entries aggregated into this daily value.
 */
data class PreparedMeasurement(
    val timestamp: Long,
    val elapsedDays: Double,
    val weightKg: Double,
    val originalMeasurementCount: Int = 1
)

/**
 * Diagnostic record produced during data preparation.
 */
data class DataPreparationDiagnostics(
    val acceptedRawCount: Int,
    val rejectedRawCount: Int,
    val aggregatedDailyCount: Int,
    val rejectionReasons: List<String>
)

/**
 * Output wrapper containing normalized prepared measurements and preparation diagnostics.
 */
data class PreparedDataset(
    val measurements: List<PreparedMeasurement>,
    val diagnostics: DataPreparationDiagnostics
)
