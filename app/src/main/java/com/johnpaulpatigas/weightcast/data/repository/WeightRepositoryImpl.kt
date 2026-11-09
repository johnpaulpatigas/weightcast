package com.johnpaulpatigas.weightcast.data.repository

import com.johnpaulpatigas.weightcast.data.local.dao.WeightEntryDao
import com.johnpaulpatigas.weightcast.data.mapper.toDomain
import com.johnpaulpatigas.weightcast.data.mapper.toEntity
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightEntryDao
) : WeightRepository {

    override fun getAllEntries(): Flow<List<WeightEntry>> {
        return dao.getAllEntriesSortedByTimestampDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: Long): WeightEntry? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getEntryByTimestamp(timestamp: Long): WeightEntry? {
        return dao.getByTimestamp(timestamp)?.toDomain()
    }

    override suspend fun addEntry(entry: WeightEntry): Long {
        return dao.insert(entry.toEntity())
    }

    override suspend fun updateEntry(entry: WeightEntry) {
        dao.update(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: WeightEntry) {
        dao.delete(entry.toEntity())
    }

    override suspend fun deleteEntryById(id: Long) {
        dao.deleteById(id)
    }
}
