package com.johnpaulpatigas.weightcast.domain.repository

import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoal(): Flow<WeightGoal?>
    suspend fun saveGoal(goal: WeightGoal)
    suspend fun clearGoal()
}
