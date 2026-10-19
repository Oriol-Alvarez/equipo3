package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockSuggestionsRepositoryImpl
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SuggestionsViewModelTest {

    private lateinit var suggestionsRepository: MockSuggestionsRepositoryImpl
    private lateinit var viewModel: SuggestionsViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        suggestionsRepository = MockSuggestionsRepositoryImpl()
        viewModel = SuggestionsViewModel(suggestionsRepository, testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun initialState_loadsAllSuggestionsAndCategorizesCounts() {
        val state = viewModel.uiState.value

        assertTrue(state.totalCount >= 2)
        assertEquals(state.totalCount, state.pendingCount + state.inReviewCount + state.attendedCount)
        assertTrue("Debe haber al menos 1 sugerencia en revisión inicialmente (sug_2)", state.inReviewCount >= 1)

        val inReviewItem = state.suggestions.firstOrNull { it.id == "sug_2" }
        assertNotNull(inReviewItem)
        assertEquals(SuggestionStatus.EN_REVISION, inReviewItem?.status)
    }

    @Test
    fun respondSuggestion_marksInProgressSuggestionAsAttended() {
        val initialInReview = viewModel.uiState.value.inReviewCount
        val initialAttended = viewModel.uiState.value.attendedCount

        val resolutionNote = "Toldo instalado exitosamente en el arenero exterior."
        viewModel.respondSuggestion(
            suggestionId = "sug_2",
            responseText = resolutionNote,
            newStatus = SuggestionStatus.ATENDIDA
        )

        // Verificar repositorio
        val updatedInRepo = suggestionsRepository.suggestions.value.first { it.id == "sug_2" }
        assertEquals(SuggestionStatus.ATENDIDA, updatedInRepo.status)
        assertEquals(resolutionNote, updatedInRepo.response)
        assertEquals("Laura Méndez (Dirección)", updatedInRepo.responderName)
        assertEquals("Hoy, ahora", updatedInRepo.responseDate)

        // Verificar ViewModel UiState reactivo
        val updatedState = viewModel.uiState.value
        assertEquals(initialInReview - 1, updatedState.inReviewCount)
        assertEquals(initialAttended + 1, updatedState.attendedCount)

        val updatedInState = updatedState.suggestions.first { it.id == "sug_2" }
        assertEquals(SuggestionStatus.ATENDIDA, updatedInState.status)
    }

    @Test
    fun respondSuggestion_movesPendingToInProgress() {
        val initialPending = viewModel.uiState.value.pendingCount
        val initialInReview = viewModel.uiState.value.inReviewCount

        viewModel.respondSuggestion(
            suggestionId = "sug_1",
            responseText = "Evaluando propuesta con el equipo de cocina.",
            newStatus = SuggestionStatus.EN_REVISION
        )

        val updatedState = viewModel.uiState.value
        assertEquals(initialPending - 1, updatedState.pendingCount)
        assertEquals(initialInReview + 1, updatedState.inReviewCount)

        val updatedInRepo = suggestionsRepository.suggestions.value.first { it.id == "sug_1" }
        assertEquals(SuggestionStatus.EN_REVISION, updatedInRepo.status)
    }

    @Test
    fun filterByStatus_showsOnlyAttendedSuggestions() {
        // Marcamos sug_2 como atendida primero
        viewModel.respondSuggestion(
            suggestionId = "sug_2",
            responseText = "Resuelto",
            newStatus = SuggestionStatus.ATENDIDA
        )

        // Aplicamos filtro de ATENDIDA
        viewModel.onStatusFilterSelected(SuggestionStatus.ATENDIDA)

        val state = viewModel.uiState.value
        assertEquals(SuggestionStatus.ATENDIDA, state.selectedStatusFilter)
        assertTrue(state.filteredSuggestions.isNotEmpty())
        assertTrue(state.filteredSuggestions.all { it.status == SuggestionStatus.ATENDIDA })
    }
}
