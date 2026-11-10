package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.model.DashboardSummary
import com.johnpaulpatigas.weightcast.domain.model.GoalType
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import com.johnpaulpatigas.weightcast.domain.trend.engine.StatisticalTrendEvaluator
import com.johnpaulpatigas.weightcast.domain.trend.model.DataSufficiency
import com.johnpaulpatigas.weightcast.domain.trend.model.TrendStability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository
) {
    operator fun invoke(): Flow<DashboardSummary> {
        return combine(
            weightRepository.getAllEntries(),
            goalRepository.getGoal()
        ) { entries, goal ->
            if (entries.isEmpty()) {
                return@combine DashboardSummary(targetGoal = goal)
            }

            val sortedAsc = entries.sortedBy { it.timestamp }
            val latestEntry = sortedAsc.last()
            val currentKg = latestEntry.weightInKg
            val latestTimestamp = latestEntry.timestamp

            val trendResult = StatisticalTrendEvaluator.evaluateTrend(
                rawEntries = sortedAsc,
                evaluationTimestamp = latestTimestamp
            )

            val estimate = trendResult.estimate
            val trendKg = estimate?.currentTrendKg
            val weeklyChangeKg = estimate?.weeklyChangeKg
            val sufficiency = trendResult.sufficiency
            val stability = estimate?.trendStability ?: TrendStability.UNSTABLE

            val (progressPct, targetDateMillis) = calculateGoalMetrics(
                entries = sortedAsc,
                currentKg = currentKg,
                weeklyChangeKg = weeklyChangeKg,
                sufficiency = sufficiency,
                goal = goal
            )

            DashboardSummary(
                currentWeightInKg = currentKg,
                smoothedTrendWeightInKg = trendKg,
                weeklyChangeKg = weeklyChangeKg,
                trendEstimate = estimate,
                dataSufficiency = sufficiency,
                trendStability = stability,
                trendDiagnostics = trendResult.diagnostics,
                targetGoal = goal,
                targetProgressPercentage = progressPct,
                estimatedTargetDateMillis = targetDateMillis,
                recentEntries = entries.take(5)
            )
        }
    }

    private fun calculateGoalMetrics(
        entries: List<WeightEntry>,
        currentKg: Double,
        weeklyChangeKg: Double?,
        sufficiency: DataSufficiency,
        goal: WeightGoal?
    ): Pair<Float?, Long?> {
        if (goal == null || entries.isEmpty()) return Pair(null, null)

        val firstEntry = entries.first()
        val startKg = firstEntry.weightInKg
        val targetKg = goal.targetWeightInKg

        val totalNeeded = targetKg - startKg
        if (totalNeeded == 0.0) return Pair(1.0f, null)

        val totalAchieved = currentKg - startKg
        val rawProgress = (totalAchieved / totalNeeded).toFloat()
        val clampedProgress = rawProgress.coerceIn(0.0f, 1.0f)

        var targetDate: Long? = null
        if (weeklyChangeKg != null && weeklyChangeKg != 0.0 && sufficiency != DataSufficiency.INSUFFICIENT_DATA) {
            val dailyRate = weeklyChangeKg / 7.0
            val remainingKg = targetKg - currentKg

            val isMovingTowardsGoal = when (goal.goalType) {
                GoalType.WEIGHT_LOSS -> dailyRate < 0 && remainingKg < 0
                GoalType.WEIGHT_GAIN -> dailyRate > 0 && remainingKg > 0
                GoalType.MAINTENANCE -> false
            }

            if (isMovingTowardsGoal) {
                val daysToTarget = (remainingKg / dailyRate).toLong()
                if (daysToTarget in 1..3650) {
                    targetDate = System.currentTimeMillis() + (daysToTarget * 24 * 60 * 60 * 1000L)
                }
            }
        }

        return Pair(clampedProgress, targetDate)
    }
}
