package com.johnpaulpatigas.weightcast.feature.weight_logging

import com.johnpaulpatigas.weightcast.domain.model.WeightEntry

data class WeightLoggingUiState(
    val entries: List<WeightEntry> = emptyList(),
    val isFormOpen: Boolean = false,
    val isEditing: Boolean = false,
    val editingEntryId: Long? = null,
    val weightInput: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notesInput: String = "",
    val validationError: String? = null,
    val duplicateWarningEntry: WeightEntry? = null,
    val isSavedSuccessfully: Boolean = false,
    val isLoading: Boolean = false
)
