package com.johnpaulpatigas.weightcast.domain.trend.engine

object MovingAverageEstimator {

    private const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L

    /**
     * Calculates trailing 7-calendar-day moving average at a specific evaluation timestamp.
     *
     * Rules:
     * 1. Window bounds: [evaluationTimestamp - 7 days, evaluationTimestamp] (inclusive).
     * 2. Uses ONLY measurements available on or before evaluationTimestamp (strictly no future data).
     * 3. Uses daily representative measurements from [PreparedMeasurement].
     * 4. Returns arithmetic mean of daily representatives present in window.
     * 5. Returns null if zero measurements exist in window.
     */
    fun calculateMovingAverageAt(
        preparedMeasurements: List<PreparedMeasurement>,
        evaluationTimestamp: Long
    ): Double? {
        val windowStart = evaluationTimestamp - SEVEN_DAYS_MILLIS
        val windowMeasurements = preparedMeasurements.filter {
            it.timestamp in windowStart..evaluationTimestamp
        }

        if (windowMeasurements.isEmpty()) return null

        return windowMeasurements.map { it.weightKg }.average()
    }

    /**
     * Calculates trailing 7-day moving average for all prepared measurements in dataset.
     * Returns list of pairs (timestamp, movingAverageWeightKg).
     */
    fun calculateMovingAverageSeries(
        preparedMeasurements: List<PreparedMeasurement>
    ): List<Pair<Long, Double>> {
        if (preparedMeasurements.isEmpty()) return emptyList()

        val sorted = preparedMeasurements.sortedBy { it.timestamp }
        return sorted.mapNotNull { measurement ->
            val avg = calculateMovingAverageAt(sorted, measurement.timestamp)
            if (avg != null) Pair(measurement.timestamp, avg) else null
        }
    }
}
