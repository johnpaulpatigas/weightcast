package com.johnpaulpatigas.weightcast.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.preferences.UserPreferencesRepository
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.usecase.GetDashboardSummaryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetHistoricalChartDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getHistoricalChartDataUseCase: GetHistoricalChartDataUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.DAYS_30)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeUserPreferences()
        observeDashboardSummary()
        observeChartData()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.preferredUnit.collect { unit ->
                _uiState.update { it.copy(displayUnit = unit) }
            }
        }
    }

    private fun observeDashboardSummary() {
        viewModelScope.launch {
            getDashboardSummaryUseCase().collect { summary ->
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            }
        }
    }

    private fun observeChartData() {
        viewModelScope.launch {
            _selectedTimeRange.flatMapLatest { range ->
                getHistoricalChartDataUseCase(range)
            }.collect { points ->
                _uiState.update { it.copy(chartPoints = points) }
            }
        }
    }

    fun onTimeRangeSelected(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
        _uiState.update { it.copy(selectedTimeRange = timeRange) }
    }

    fun onUnitSelected(unit: WeightUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredUnit(unit)
        }
    }
}
