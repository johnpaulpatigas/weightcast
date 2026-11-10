package com.johnpaulpatigas.weightcast.feature.weight_logging

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.preferences.UserPreferencesRepository
import com.johnpaulpatigas.weightcast.domain.usecase.AddWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.DeleteWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.FakeWeightRepository
import com.johnpaulpatigas.weightcast.domain.usecase.GetWeightEntriesUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.UpdateWeightEntryUseCase
import com.johnpaulpatigas.weightcast.domain.usecase.ValidateWeightEntryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _unit = MutableStateFlow(WeightUnit.KG)
    override val preferredUnit: Flow<WeightUnit> = _unit

    override suspend fun setPreferredUnit(unit: WeightUnit) {
        _unit.value = unit
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeightLoggingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeWeightRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: WeightLoggingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWeightRepository()
        userPreferencesRepository = FakeUserPreferencesRepository()

        val validateUseCase = ValidateWeightEntryUseCase(repository)
        val addUseCase = AddWeightEntryUseCase(repository, validateUseCase)
        val updateUseCase = UpdateWeightEntryUseCase(repository, validateUseCase)
        val deleteUseCase = DeleteWeightEntryUseCase(repository)
        val getUseCase = GetWeightEntriesUseCase(repository)

        viewModel = WeightLoggingViewModel(
            getWeightEntriesUseCase = getUseCase,
            addWeightEntryUseCase = addUseCase,
            updateWeightEntryUseCase = updateUseCase,
            deleteWeightEntryUseCase = deleteUseCase,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows empty entries and form closed`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.entries.isEmpty())
        assertFalse(state.isFormOpen)
        assertFalse(state.isEditing)
    }

    @Test
    fun `onOpenAddForm opens form with reset state`() = runTest {
        viewModel.onOpenAddForm()
        val state = viewModel.uiState.value
        assertTrue(state.isFormOpen)
        assertFalse(state.isEditing)
        assertEquals("", state.weightInput)
    }

    @Test
    fun `onSaveEntry with invalid input sets validation error`() = runTest {
        viewModel.onOpenAddForm()
        viewModel.onWeightInputChanged("-10.0")
        viewModel.onSaveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertFalse(state.isSavedSuccessfully)
    }

    @Test
    fun `onSaveEntry with valid input saves entry`() = runTest {
        viewModel.onOpenAddForm()
        viewModel.onWeightInputChanged("72.5")
        viewModel.onUnitSelected(WeightUnit.KG)
        viewModel.onSaveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSavedSuccessfully)
        assertFalse(state.isFormOpen)
        assertEquals(1, state.entries.size)
        assertEquals(72.5, state.entries.first().weightInKg, 0.001)
    }

    @Test
    fun `onDeleteEntry removes entry from repository`() = runTest {
        viewModel.onOpenAddForm()
        viewModel.onWeightInputChanged("80.0")
        viewModel.onSaveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        val savedId = viewModel.uiState.value.entries.first().id
        viewModel.onDeleteEntry(savedId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }
}
