package com.johnpaulpatigas.weightcast.feature.goals_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.domain.model.GoalType
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
    val selectedGoalType: GoalType = GoalType.WEIGHT_LOSS,
    val isGoalSaved: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val exportStatusMessage: String? = null
)

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
                    _uiState.update {
                        it.copy(
                            targetWeightInput = goal.targetWeightInKg.toString(),
                            selectedGoalType = goal.goalType,
                            isGoalSaved = true
                        )
                    }
                }
            }
        }
    }

    fun onTargetWeightChanged(input: String) {
        _uiState.update { it.copy(targetWeightInput = input, isGoalSaved = false) }
    }

    fun onGoalTypeChanged(type: GoalType) {
        _uiState.update { it.copy(selectedGoalType = type, isGoalSaved = false) }
    }

    fun onSaveGoal() {
        val targetKg = _uiState.value.targetWeightInput.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val goal = WeightGoal(
                targetWeightInKg = targetKg,
                unit = WeightUnit.KG,
                goalType = _uiState.value.selectedGoalType
            )
            goalRepository.saveGoal(goal)
            _uiState.update { it.copy(isGoalSaved = true) }
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
