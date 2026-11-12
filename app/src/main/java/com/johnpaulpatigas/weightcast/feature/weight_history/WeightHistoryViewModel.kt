package com.johnpaulpatigas.weightcast.feature.weight_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnpaulpatigas.weightcast.core.util.TrendCalculator
import com.johnpaulpatigas.weightcast.domain.model.TimeRange
import com.johnpaulpatigas.weightcast.domain.usecase.DeleteWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetHistoricalChartDataUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.GetWeightEntriesUseCase
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
class WeightHistoryViewModel @Inject constructor(
    private val getWeightEntriesUseCase: GetWeightEntriesUseCase,
    private val getHistoricalChartDataUseCase: GetHistoricalChartDataUseCase,
    private val deleteWeightEntryUseCase: DeleteWeightEntryUseCase
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEKS_1)

    private val _uiState = MutableStateFlow(WeightHistoryUiState())
    val uiState: StateFlow<WeightHistoryUiState> = _uiState.asStateFlow()

    init {
        observeEntries()
        observeChartData()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            getWeightEntriesUseCase().collect { entries ->
                val filtered = TrendCalculator.filterEntriesByTimeRange(entries, _selectedTimeRange.value)
                    .sortedByDescending { it.timestamp }

                _uiState.update {
                    it.copy(
                        allEntries = entries,
                        filteredEntries = filtered,
                        isLoading = false
                    )
                }
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

    fun onTimeRangeSelected(range: TimeRange) {
        _selectedTimeRange.value = range
        val filtered = TrendCalculator.filterEntriesByTimeRange(_uiState.value.allEntries, range)
            .sortedByDescending { it.timestamp }

        _uiState.update {
            it.copy(
                selectedTimeRange = range,
                filteredEntries = filtered
            )
        }
    }

    fun onDeleteEntry(id: Long) {
        viewModelScope.launch {
            deleteWeightEntryUseCase(id)
        }
    }
}
