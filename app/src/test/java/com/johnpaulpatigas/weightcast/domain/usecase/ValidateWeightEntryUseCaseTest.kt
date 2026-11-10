package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWeightRepository : WeightRepository {
    private val entries = mutableMapOf<Long, WeightEntry>()
    private var idCounter = 1L
    private val _flow = MutableStateFlow<List<WeightEntry>>(emptyList())

    private fun updateFlow() {
        _flow.value = entries.values.sortedByDescending { it.timestamp }
    }

    override fun getAllEntries(): Flow<List<WeightEntry>> = _flow

    override suspend fun getEntryById(id: Long): WeightEntry? = entries[id]

    override suspend fun getEntryByTimestamp(timestamp: Long): WeightEntry? {
        return entries.values.find { it.timestamp == timestamp }
    }

    override suspend fun addEntry(entry: WeightEntry): Long {
        val id = if (entry.id == 0L) idCounter++ else entry.id
        val saved = entry.copy(id = id)
        entries[id] = saved
        updateFlow()
        return id
    }

    override suspend fun updateEntry(entry: WeightEntry) {
        entries[entry.id] = entry
        updateFlow()
    }

    override suspend fun deleteEntry(entry: WeightEntry) {
        entries.remove(entry.id)
        updateFlow()
    }

    override suspend fun deleteEntryById(id: Long) {
        entries.remove(id)
        updateFlow()
    }

    fun clear() {
        entries.clear()
        idCounter = 1L
        updateFlow()
    }
}

class ValidateWeightEntryUseCaseTest {

    private lateinit var repository: FakeWeightRepository
    private lateinit var validateUseCase: ValidateWeightEntryUseCase

    @Before
    fun setUp() {
        repository = FakeWeightRepository()
        validateUseCase = ValidateWeightEntryUseCase(repository)
    }

    @Test
    fun `null or non-positive weight returns InvalidWeight`() = runTest {
        val res1 = validateUseCase(null, WeightUnit.KG, 1000L)
        assertTrue(res1 is ValidationResult.InvalidWeight)

        val res2 = validateUseCase(0.0, WeightUnit.KG, 1000L)
        assertTrue(res2 is ValidationResult.InvalidWeight)

        val res3 = validateUseCase(-5.0, WeightUnit.KG, 1000L)
        assertTrue(res3 is ValidationResult.InvalidWeight)
    }

    @Test
    fun `implausible weight returns InvalidWeight`() = runTest {
        val resLow = validateUseCase(1.0, WeightUnit.KG, 1000L)
        assertTrue(resLow is ValidationResult.InvalidWeight)

        val resHigh = validateUseCase(600.0, WeightUnit.KG, 1000L)
        assertTrue(resHigh is ValidationResult.InvalidWeight)
    }

    @Test
    fun `future date returns FutureDate`() = runTest {
        val futureTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 mins in future
        val result = validateUseCase(70.0, WeightUnit.KG, futureTime)
        assertTrue(result is ValidationResult.FutureDate)
    }

    @Test
    fun `duplicate timestamp returns DuplicateTimestamp when not allowed`() = runTest {
        val ts = 1700000000000L
        repository.addEntry(WeightEntry(weightInKg = 70.0, originalUnit = WeightUnit.KG, timestamp = ts))

        val result = validateUseCase(72.0, WeightUnit.KG, ts, allowDuplicate = false)
        assertTrue(result is ValidationResult.DuplicateTimestamp)
    }

    @Test
    fun `valid entry returns Success`() = runTest {
        val now = System.currentTimeMillis()
        val result = validateUseCase(75.5, WeightUnit.KG, now)
        assertTrue(result is ValidationResult.Success)
    }
}
