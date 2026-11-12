package com.johnpaulpatigas.weightcast.domain.model

enum class TimeRange(val label: String, val days: Int) {
    WEEKS_1("1W", 7),
    MONTHS_1("1M", 30),
    MONTHS_3("3M", 90),
    YEAR_1("1Y", 365),
    ALL_TIME("All", -1);

    fun getStartTimestamp(currentMillis: Long = System.currentTimeMillis()): Long {
        if (days < 0) return 0L
        return currentMillis - (days.toLong() * 24 * 60 * 60 * 1000L)
    }
}
