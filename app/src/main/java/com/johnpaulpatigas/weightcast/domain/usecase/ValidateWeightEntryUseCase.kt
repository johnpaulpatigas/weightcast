package com.johnpaulpatigas.weightcast.domain.usecase

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.core.util.WeightValidationConstants
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import javax.inject.Inject

sealed interface ValidationResult {
    data object Success : ValidationResult
    data class InvalidWeight(val message: String) : ValidationResult
    data class FutureDate(val message: String) : ValidationResult
    data class DuplicateTimestamp(val existingEntry: WeightEntry) : ValidationResult
}

class ValidateWeightEntryUseCase @Inject constructor(
    private val repository: WeightRepository
) {
    suspend operator fun invoke(
        weightValue: Double?,
        unit: WeightUnit = WeightUnit.KG,
        timestamp: Long,
        entryIdToIgnore: Long? = null,
        allowDuplicate: Boolean = false
    ): ValidationResult {
        if (weightValue == null || weightValue <= 0.0) {
            return ValidationResult.InvalidWeight("Weight must be greater than zero.")
        }

        val weightInKg = WeightConverter.toCanonicalKg(weightValue, unit)
        if (weightInKg < WeightValidationConstants.MIN_PLAUSIBLE_WEIGHT_KG || weightInKg > WeightValidationConstants.MAX_PLAUSIBLE_WEIGHT_KG) {
            return ValidationResult.InvalidWeight("Please enter a weight within valid limits (2 kg - 500 kg).")
        }

        val maxAllowedTime = System.currentTimeMillis() + (5 * 60 * 1000L)
        if (timestamp > maxAllowedTime) {
            return ValidationResult.FutureDate("Weight log date cannot be in the future.")
        }

        if (!allowDuplicate) {
            val existing = repository.getEntryByTimestamp(timestamp)
            if (existing != null && existing.id != entryIdToIgnore) {
                return ValidationResult.DuplicateTimestamp(existing)
            }
        }

        return ValidationResult.Success
    }
}
