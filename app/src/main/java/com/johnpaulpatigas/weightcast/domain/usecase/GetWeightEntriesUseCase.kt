package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWeightEntriesUseCase @Inject constructor(
    private val repository: WeightRepository
) {
    operator fun invoke(): Flow<List<WeightEntry>> {
        return repository.getAllEntries()
    }
}
