package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

object TrendCalculator {

    /**
     * Filters entries based on the selected [TimeRange].
     */
    fun filterEntriesByTimeRange(
        entries: List<WeightEntry>,
        range: TimeRange,
        nowMillis: Long? = null
    ): List<WeightEntry> {
        if (range == TimeRange.ALL_TIME || entries.isEmpty()) return entries.sortedBy { it.timestamp }

        val referenceTime = nowMillis ?: entries.maxOf { it.timestamp }
        val startTimestamp = range.getStartTimestamp(referenceTime)
        return entries
            .filter { it.timestamp >= startTimestamp }
            .sortedBy { it.timestamp }
    }
}
