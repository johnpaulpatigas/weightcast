package com.johnpaulpatigas.weightcast.domain.repository

import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun getAllEntries(): Flow<List<WeightEntry>>
    suspend fun getEntryById(id: Long): WeightEntry?
    suspend fun getEntryByTimestamp(timestamp: Long): WeightEntry?
    suspend fun addEntry(entry: WeightEntry): Long
    suspend fun updateEntry(entry: WeightEntry)
    suspend fun deleteEntry(entry: WeightEntry)
    suspend fun deleteEntryById(id: Long)
}
