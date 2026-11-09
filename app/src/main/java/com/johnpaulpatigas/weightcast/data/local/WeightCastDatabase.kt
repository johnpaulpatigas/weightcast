package com.johnpaulpatigas.weightcast.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.johnpaulpatigas.weightcast.data.local.dao.WeightEntryDao
import com.johnpaulpatigas.weightcast.data.local.dao.WeightGoalDao
import com.johnpaulpatigas.weightcast.data.local.entity.WeightEntryEntity
import com.johnpaulpatigas.weightcast.data.local.entity.WeightGoalEntity

@Database(
    entities = [
        WeightEntryEntity::class,
        WeightGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WeightCastDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun weightGoalDao(): WeightGoalDao
}
