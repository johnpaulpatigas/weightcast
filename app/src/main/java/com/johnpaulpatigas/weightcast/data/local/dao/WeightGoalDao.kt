package com.johnpaulpatigas.weightcast.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.johnpaulpatigas.weightcast.data.local.entity.WeightGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: WeightGoalEntity)

    @Query("SELECT * FROM weight_goals WHERE id = 1 LIMIT 1")
    fun getGoal(): Flow<WeightGoalEntity?>

    @Query("SELECT * FROM weight_goals WHERE id = 1 LIMIT 1")
    suspend fun getGoalDirect(): WeightGoalEntity?

    @Query("DELETE FROM weight_goals")
    suspend fun clearGoal()
}
