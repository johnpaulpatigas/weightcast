package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import javax.inject.Inject

class DeleteWeightEntryUseCase @Inject constructor(
    private val repository: WeightRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteEntryById(id)
    }
}
