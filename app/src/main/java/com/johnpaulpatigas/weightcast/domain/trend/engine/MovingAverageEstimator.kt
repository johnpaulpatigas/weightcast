package com.johnpaulpatigas.weightcast.domain.trend.engine

object MovingAverageEstimator {

    /**
     * Calculates trailing 7-calendar-date moving average at a specific evaluation timestamp.
     *
     * Window Semantics:
     * - Include evaluation date and 6 preceding UTC calendar dates: `[evalUtcDay - 6 days, evalUtcDay]`.
     * - Exclude observations before the beginning of the 7-calendar-date window.
     * - Exclude observations strictly after [evaluationTimestamp] (zero future-data leakage).
     * - Uses arithmetic mean of available daily representatives within window.
     * - Returns null if window contains zero daily representatives.
     */
    fun calculateMovingAverageAt(
        preparedMeasurements: List<PreparedMeasurement>,
        evaluationTimestamp: Long
    ): Double? {
        val evalUtcDay = evaluationTimestamp / 86_400_000L
        val windowStartUtcDay = evalUtcDay - 6L

        val windowMeasurements = preparedMeasurements.filter { measurement ->
            val measurementUtcDay = measurement.timestamp / 86_400_000L
            measurementUtcDay in windowStartUtcDay..evalUtcDay && measurement.timestamp <= evaluationTimestamp
        }

        if (windowMeasurements.isEmpty()) return null

        return windowMeasurements.map { it.weightKg }.average()
    }

    /**
     * Calculates trailing 7-calendar-date moving average for all prepared measurements in dataset.
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
