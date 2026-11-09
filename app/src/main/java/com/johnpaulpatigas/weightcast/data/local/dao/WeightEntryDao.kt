package com.johnpaulpatigas.weightcast.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.johnpaulpatigas.weightcast.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity): Long

    @Update
    suspend fun update(entry: WeightEntryEntity)

    @Delete
    suspend fun delete(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC")
    fun getAllEntriesSortedByTimestampDesc(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp ASC")
    fun getAllEntriesSortedByTimestampAsc(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getByTimestamp(timestamp: Long): WeightEntryEntity?
}
