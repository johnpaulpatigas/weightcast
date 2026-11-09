package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import javax.inject.Inject

class DuplicateTimestampException(val existingEntry: WeightEntry) :
    Exception("A weight measurement already exists at this timestamp.")

class AddWeightEntryUseCase @Inject constructor(
    private val repository: WeightRepository,
    private val validateUseCase: ValidateWeightEntryUseCase
) {
    suspend operator fun invoke(
        weightValue: Double,
        unit: WeightUnit,
        timestamp: Long,
        notes: String? = null,
        allowDuplicate: Boolean = false
    ): Result<Long> {
        val validation = validateUseCase(
            weightValue = weightValue,
            unit = unit,
            timestamp = timestamp,
            allowDuplicate = allowDuplicate
        )

        return when (validation) {
            is ValidationResult.Success -> {
                val canonicalKg = WeightConverter.toCanonicalKg(weightValue, unit)
                val entry = WeightEntry(
                    weightInKg = canonicalKg,
                    originalUnit = unit,
                    timestamp = timestamp,
                    notes = notes?.trim()?.ifBlank { null }
                )
                Result.success(repository.addEntry(entry))
            }
            is ValidationResult.InvalidWeight -> Result.failure(IllegalArgumentException(validation.message))
            is ValidationResult.FutureDate -> Result.failure(IllegalArgumentException(validation.message))
            is ValidationResult.DuplicateTimestamp -> Result.failure(DuplicateTimestampException(validation.existingEntry))
        }
    }
}
