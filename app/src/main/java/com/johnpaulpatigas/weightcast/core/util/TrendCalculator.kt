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
        nowMillis: Long = System.currentTimeMillis()
    ): List<WeightEntry> {
        if (range == TimeRange.ALL_TIME) return entries.sortedBy { it.timestamp }

        val startTimestamp = range.getStartTimestamp(nowMillis)
        return entries
            .filter { it.timestamp >= startTimestamp }
            .sortedBy { it.timestamp }
    }
}
