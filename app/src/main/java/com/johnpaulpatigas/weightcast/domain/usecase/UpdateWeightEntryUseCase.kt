package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import javax.inject.Inject

class UpdateWeightEntryUseCase @Inject constructor(
    private val repository: WeightRepository,
    private val validateUseCase: ValidateWeightEntryUseCase
) {
    suspend operator fun invoke(
        id: Long,
        weightValue: Double,
        unit: WeightUnit,
        timestamp: Long,
        notes: String? = null,
        allowDuplicate: Boolean = true
    ): Result<Unit> {
        val validation = validateUseCase(
            weightValue = weightValue,
            unit = unit,
            timestamp = timestamp,
            entryIdToIgnore = id,
            allowDuplicate = allowDuplicate
        )

        return when (validation) {
            is ValidationResult.Success -> {
                val existing = repository.getEntryById(id)
                    ?: return Result.failure(IllegalArgumentException("Entry with id $id not found."))

                val canonicalKg = WeightConverter.toCanonicalKg(weightValue, unit)
                val updated = existing.copy(
                    weightInKg = canonicalKg,
                    originalUnit = unit,
                    timestamp = timestamp,
                    notes = notes?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateEntry(updated)
                Result.success(Unit)
            }
            is ValidationResult.InvalidWeight -> Result.failure(IllegalArgumentException(validation.message))
            is ValidationResult.FutureDate -> Result.failure(IllegalArgumentException(validation.message))
            is ValidationResult.DuplicateTimestamp -> Result.failure(DuplicateTimestampException(validation.existingEntry))
        }
    }
}
