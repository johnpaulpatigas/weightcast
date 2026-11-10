package com.johnpaulpatigas.weightcast.feature.goals_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.WeightGoal
import com.johnpaulpatigas.weightcast.domain.repository.GoalRepository
import com.johnpaulpatigas.weightcast.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsSettingsUiState(
    val targetWeightInput: String = "",
    val savedTargetWeight: String = "",
    val dailyReminderEnabled: Boolean = false,
    val exportStatusMessage: String? = null
) {
    val showSaveButton: Boolean
        get() {
            val inputVal = targetWeightInput.trim().toDoubleOrNull()
            val savedVal = savedTargetWeight.trim().toDoubleOrNull()
            return inputVal != null && inputVal > 0.0 && inputVal != savedVal
        }
}

@HiltViewModel
class GoalsSettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsSettingsUiState())
    val uiState: StateFlow<GoalsSettingsUiState> = _uiState.asStateFlow()

    init {
        observeGoal()
    }

    private fun observeGoal() {
        viewModelScope.launch {
            goalRepository.getGoal().collect { goal ->
                if (goal != null) {
                    val formatted = WeightConverter.roundTo(goal.targetWeightInKg, 2).toString()
                    _uiState.update {
                        it.copy(
                            targetWeightInput = formatted,
                            savedTargetWeight = formatted
                        )
                    }
                }
            }
        }
    }

    fun onTargetWeightChanged(input: String) {
        _uiState.update { it.copy(targetWeightInput = input) }
    }

    fun onSaveGoal() {
        val targetKg = _uiState.value.targetWeightInput.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val goal = WeightGoal(
                targetWeightInKg = targetKg,
                unit = WeightUnit.KG
            )
            goalRepository.saveGoal(goal)
            val formatted = WeightConverter.roundTo(targetKg, 2).toString()
            _uiState.update {
                it.copy(
                    savedTargetWeight = formatted,
                    targetWeightInput = formatted
                )
            }
        }
    }

    fun onToggleDailyReminder(enabled: Boolean) {
        _uiState.update { it.copy(dailyReminderEnabled = enabled) }
    }

    fun onExportData() {
        viewModelScope.launch {
            val entries = weightRepository.getAllEntries().first()
            _uiState.update {
                it.copy(exportStatusMessage = "Successfully prepared export containing ${entries.size} weight entries in KG.")
            }
        }
    }

    fun clearExportStatus() {
        _uiState.update { it.copy(exportStatusMessage = null) }
    }
}
