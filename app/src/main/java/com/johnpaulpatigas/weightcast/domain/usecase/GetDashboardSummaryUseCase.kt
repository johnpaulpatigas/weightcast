package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.domain.model.DashboardSummary
import com.johnpaulpatigas.weightcast.domain.model.GoalType
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
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

            val maTrendList = TrendCalculator.calculate7DayMovingAverage(sortedAsc)
            val latestTrendKg = maTrendList.lastOrNull()?.second

            val change7d = TrendCalculator.calculateWeightChange(sortedAsc, days = 7)

            val (progressPct, targetDateMillis) = calculateGoalMetrics(
                entries = sortedAsc,
                currentKg = currentKg,
                change7d = change7d,
                goal = goal
            )

            DashboardSummary(
                currentWeightInKg = currentKg,
                smoothedTrendWeightInKg = latestTrendKg ?: currentKg,
                weightChange7dInKg = change7d,
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
        change7d: Double?,
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
        if (change7d != null && change7d != 0.0) {
            val dailyRate = change7d / 7.0
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
