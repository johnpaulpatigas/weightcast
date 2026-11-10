package com.johnpaulpatigas.weightcast.domain.trend.engine

import com.johnpaulpatigas.weightcast.core.util.WeightValidationConstants
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

object DataPreparation {

    const val MILLIS_PER_DAY = 86_400_000.0

    /**
     * Processes raw weight entries into clean, chronologically sorted, daily-aggregated prepared measurements.
     *
     * Rules:
     * 1. Rejects invalid weights (<= 0.0, NaN, Infinite, or outside product limits [2.0 kg, 500.0 kg]).
     * 2. Sorts entries chronologically by timestamp.
     * 3. Aggregates multiple entries occurring on the same UTC calendar day using arithmetic mean.
     * 4. Normalizes elapsed days relative to dataset baseline t0: t = (timestamp - t0) / 86,400,000.0.
     * 5. Does NOT fabricate missing days.
     */
    fun prepare(rawEntries: List<WeightEntry>): PreparedDataset {
        if (rawEntries.isEmpty()) {
            return PreparedDataset(
                measurements = emptyList(),
                diagnostics = DataPreparationDiagnostics(
                    acceptedRawCount = 0,
                    rejectedRawCount = 0,
                    aggregatedDailyCount = 0,
                    rejectionReasons = emptyList()
                )
            )
        }

        val rejectionReasons = mutableListOf<String>()
        var rejectedCount = 0
        val validEntries = mutableListOf<WeightEntry>()

        for (entry in rawEntries) {
            val kg = entry.weightInKg
            if (kg.isNaN() || kg.isInfinite() || kg <= 0.0) {
                rejectedCount++
                rejectionReasons.add("Entry ID ${entry.id}: non-positive or non-finite weight ($kg kg)")
            } else if (kg < WeightValidationConstants.MIN_PLAUSIBLE_WEIGHT_KG || kg > WeightValidationConstants.MAX_PLAUSIBLE_WEIGHT_KG) {
                rejectedCount++
                rejectionReasons.add("Entry ID ${entry.id}: outside product validation limits ($kg kg)")
            } else {
                validEntries.add(entry)
            }
        }

        if (validEntries.isEmpty()) {
            return PreparedDataset(
                measurements = emptyList(),
                diagnostics = DataPreparationDiagnostics(
                    acceptedRawCount = 0,
                    rejectedRawCount = rejectedCount,
                    aggregatedDailyCount = 0,
                    rejectionReasons = rejectionReasons
                )
            )
        }

        val sortedValid = validEntries.sortedBy { it.timestamp }

        val groupedByUtcDay = sortedValid.groupBy { it.timestamp / 86_400_000L }

        val firstUtcDay = groupedByUtcDay.keys.minOrNull() ?: 0L
        val baselineTimestamp = firstUtcDay * 86_400_000L

        val preparedList = groupedByUtcDay.entries.map { (utcDay, dayEntries) ->
            val dayTimestamp = utcDay * 86_400_000L
            val avgWeightKg = dayEntries.map { it.weightInKg }.average()
            val elapsed = (dayTimestamp - baselineTimestamp) / MILLIS_PER_DAY

            PreparedMeasurement(
                timestamp = dayTimestamp,
                elapsedDays = elapsed,
                weightKg = avgWeightKg,
                originalMeasurementCount = dayEntries.size
            )
        }.sortedBy { it.timestamp }

        return PreparedDataset(
            measurements = preparedList,
            diagnostics = DataPreparationDiagnostics(
                acceptedRawCount = validEntries.size,
                rejectedRawCount = rejectedCount,
                aggregatedDailyCount = preparedList.size,
                rejectionReasons = rejectionReasons
            )
        )
    }
}
