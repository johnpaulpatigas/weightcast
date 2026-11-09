package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

object TrendCalculator {

    private const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L

    /**
     * Calculates 7-day trailing moving average for weight entries.
     * Returns list of pairs (timestamp, smoothedTrendInKg).
     */
    fun calculate7DayMovingAverage(entries: List<WeightEntry>): List<Pair<Long, Double>> {
        if (entries.isEmpty()) return emptyList()

        val sorted = entries.sortedBy { it.timestamp }
        return sorted.map { currentEntry ->
            val windowStart = currentEntry.timestamp - SEVEN_DAYS_MILLIS
            val windowEntries = sorted.filter { it.timestamp in windowStart..currentEntry.timestamp }
            val avgKg = windowEntries.map { it.weightInKg }.average()
            Pair(currentEntry.timestamp, avgKg)
        }
    }

    /**
     * Calculates net weight change in canonical KG over trailing [days] window.
     * Positive value = weight gained, Negative value = weight lost.
     */
    fun calculateWeightChange(
        entries: List<WeightEntry>,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis()
    ): Double? {
        if (entries.isEmpty()) return null
        val sorted = entries.sortedBy { it.timestamp }
        val latest = sorted.last()

        val windowStart = nowMillis - (days.toLong() * 24 * 60 * 60 * 1000L)
        val olderEntries = sorted.filter { it.timestamp <= windowStart }

        val baselineEntry = if (olderEntries.isNotEmpty()) {
            olderEntries.last()
        } else if (sorted.size > 1) {
            sorted.first()
        } else {
            return null
        }

        return latest.weightInKg - baselineEntry.weightInKg
    }

    /**
     * Filters entries based on the selected [TimeRange].
     */
    fun filterEntriesByTimeRange(
        entries: List<WeightEntry>,
        range: TimeRange,
        nowMillis: Long = System.currentTimeMillis()
    ): List<WeightEntry> {
        if (range == TimeRange.ALL_TIME) return entries.sortedBy { it.timestamp }

        val startTimestamp = range.getStartTimestamp(nowMillis)
        return entries
            .filter { it.timestamp >= startTimestamp }
            .sortedBy { it.timestamp }
    }
}
