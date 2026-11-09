package com.johnpaulpatigas.weightcast.data.repository

import com.johnpaulpatigas.weightcast.data.local.dao.WeightGoalDao
import com.johnpaulpatigas.weightcast.data.mapper.toDomain
import com.johnpaulpatigas.weightcast.data.mapper.toEntity
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val dao: WeightGoalDao
) : GoalRepository {

    override fun getGoal(): Flow<WeightGoal?> {
        return dao.getGoal().map { it?.toDomain() }
    }

    override suspend fun saveGoal(goal: WeightGoal) {
        dao.insertOrUpdate(goal.toEntity())
    }

    override suspend fun clearGoal() {
        dao.clearGoal()
    }
}
