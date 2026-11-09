package com.johnpaulpatigas.weightcast.domain.model

enum class TimeRange(val label: String, val days: Int) {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    MONTHS_6("6M", 180),
    YEAR_1("1Y", 365),
    ALL_TIME("All", -1);

    fun getStartTimestamp(currentMillis: Long = System.currentTimeMillis()): Long {
        if (days < 0) return 0L
        return currentMillis - (days.toLong() * 24 * 60 * 60 * 1000L)
    }
}
