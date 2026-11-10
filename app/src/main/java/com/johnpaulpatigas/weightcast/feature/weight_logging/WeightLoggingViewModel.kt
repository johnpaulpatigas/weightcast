package com.johnpaulpatigas.weightcast.feature.weight_logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.WeightEntry
import com.johnpaulpatigas.weightcast.domain.usecase.AddWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.DeleteWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.DuplicateTimestampException
import com.johnpaulpatigas.weightcast.domain.usecase.GetWeightEntriesUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.UpdateWeightEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightLoggingViewModel @Inject constructor(
    private val getWeightEntriesUseCase: GetWeightEntriesUseCase,
    private val addWeightEntryUseCase: AddWeightEntryUseCase,
    private val updateWeightEntryUseCase: UpdateWeightEntryUseCase,
    private val deleteWeightEntryUseCase: DeleteWeightEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightLoggingUiState())
    val uiState: StateFlow<WeightLoggingUiState> = _uiState.asStateFlow()

    init {
        observeWeightEntries()
    }

    private fun observeWeightEntries() {
        viewModelScope.launch {
            getWeightEntriesUseCase().collect { entriesList ->
                _uiState.update { currentState ->
                    val updatedState = currentState.copy(entries = entriesList, isLoading = false)
                    if (currentState.isFormOpen && !currentState.isEditing && currentState.weightInput.isEmpty()) {
                        val pastWeight = entriesList.firstOrNull()?.weightInKg
                        if (pastWeight != null) {
                            val formatted = WeightConverter.roundTo(pastWeight, 2).toString()
                            updatedState.copy(weightInput = formatted)
                        } else {
                            updatedState
                        }
                    } else {
                        updatedState
                    }
                }
            }
        }
    }

    fun onOpenAddForm() {
        val lastWeight = _uiState.value.entries.firstOrNull()?.weightInKg
        val initialWeightInput = lastWeight?.let { WeightConverter.roundTo(it, 2).toString() } ?: ""

        _uiState.update {
            it.copy(
                isFormOpen = true,
                isEditing = false,
                editingEntryId = null,
                weightInput = initialWeightInput,
                timestamp = System.currentTimeMillis(),
                notesInput = "",
                validationError = null,
                duplicateWarningEntry = null,
                isSavedSuccessfully = false
            )
        }
    }

    fun onOpenEditForm(entry: WeightEntry) {
        val displayWeight = WeightConverter.roundTo(entry.weightInKg, 2).toString()

        _uiState.update {
            it.copy(
                isFormOpen = true,
                isEditing = true,
                editingEntryId = entry.id,
                weightInput = displayWeight,
                timestamp = entry.timestamp,
                notesInput = entry.notes ?: "",
                validationError = null,
                duplicateWarningEntry = null,
                isSavedSuccessfully = false
            )
        }
    }

    fun onCloseForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                validationError = null,
                duplicateWarningEntry = null
            )
        }
    }

    fun onWeightInputChanged(input: String) {
        _uiState.update { it.copy(weightInput = input, validationError = null) }
    }

    fun onTimestampSelected(timestamp: Long) {
        val now = System.currentTimeMillis() + 1000L
        if (timestamp > now) {
            _uiState.update {
                it.copy(
                    timestamp = System.currentTimeMillis(),
                    validationError = "Weight log date/time cannot be in the future."
                )
            }
        } else {
            _uiState.update { it.copy(timestamp = timestamp, validationError = null) }
        }
    }

    fun onNotesInputChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun onSaveEntry(allowDuplicate: Boolean = false) {
        val state = _uiState.value
        val weightVal = state.weightInput.toDoubleOrNull()

        if (weightVal == null || weightVal <= 0.0) {
            _uiState.update { it.copy(validationError = "Please enter a valid positive weight.") }
            return
        }

        val now = System.currentTimeMillis() + 1000L
        if (state.timestamp > now) {
            _uiState.update { it.copy(validationError = "Weight log date cannot be in the future.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, validationError = null) }

            val result = if (state.isEditing && state.editingEntryId != null) {
                updateWeightEntryUseCase(
                    id = state.editingEntryId,
                    weightValue = weightVal,
                    unit = WeightUnit.KG,
                    timestamp = state.timestamp,
                    notes = state.notesInput,
                    allowDuplicate = allowDuplicate
                )
            } else {
                addWeightEntryUseCase(
                    weightValue = weightVal,
                    unit = WeightUnit.KG,
                    timestamp = state.timestamp,
                    notes = state.notesInput,
                    allowDuplicate = allowDuplicate
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFormOpen = false,
                            isSavedSuccessfully = true,
                            duplicateWarningEntry = null
                        )
                    }
                },
                onFailure = { error ->
                    if (error is DuplicateTimestampException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                duplicateWarningEntry = error.existingEntry
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                validationError = error.message ?: "Failed to save weight entry."
                            )
                        }
                    }
                }
            )
        }
    }

    fun onDeleteEntry(id: Long) {
        viewModelScope.launch {
            deleteWeightEntryUseCase(id)
            if (_uiState.value.editingEntryId == id) {
                onCloseForm()
            }
        }
    }

    fun onDismissDuplicateWarning() {
        _uiState.update { it.copy(duplicateWarningEntry = null) }
    }
}
