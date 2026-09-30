package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockSuggestionsRepositoryImpl
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParentViewModelsTest {

    private lateinit var childRepository: MockChildRepositoryImpl
    private lateinit var suggestionsRepository: MockSuggestionsRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        childRepository = MockChildRepositoryImpl()
        suggestionsRepository = MockSuggestionsRepositoryImpl()
    }

    @Test
    fun parentChildSelectionViewModel_loadsChildrenForParent() {
        val viewModel = ParentChildSelectionViewModel(childRepository)
        val children = viewModel.children.value

        assertTrue("Familiar debe tener 2 o más niños registrados", children.size >= 2)
        assertEquals("Mateo García", children[0].fullName)
        assertEquals("Lucía García López", children[1].fullName)
    }

    @Test
    fun parentHomeViewModel_initialChildAndSwitchChild() {
        val initialChild = MockDataRepository.mateoGarcia
        val sibling = MockDataRepository.luciaGarcia
        val viewModel = ParentHomeViewModel(childRepository, initialChild)

        assertEquals("Mateo García", viewModel.uiState.value.selectedChild.fullName)
        assertNotNull(viewModel.uiState.value.fullProfile)
        assertNotNull(viewModel.uiState.value.weeklySummary)

        viewModel.selectChild(sibling)

        assertEquals("Lucía García López", viewModel.uiState.value.selectedChild.fullName)
        assertEquals("Lucía García López", viewModel.uiState.value.fullProfile.child.fullName)
    }

    @Test
    fun parentSuggestionsViewModel_submitValidSuggestion_success() {
        val viewModel = ParentSuggestionsViewModel(suggestionsRepository, testScope)
        val initialCount = viewModel.uiState.value.mySuggestions.size

        val success = viewModel.sendSuggestion(
            category = SuggestionCategory.ALIMENTACION,
            subject = "Menú adaptado",
            content = "Quisiera solicitar opciones con menos sal para el almuerzo.",
            childName = "Mateo García",
            roomName = "Sala 1A"
        )

        assertTrue(success)
        assertTrue(viewModel.isSendingSuccess.value)

        val updatedSuggestions = viewModel.uiState.value.mySuggestions
        assertEquals(initialCount + 1, updatedSuggestions.size)

        val latest = updatedSuggestions.first()
        assertEquals("Menú adaptado", latest.subject)
        assertEquals(SuggestionStatus.PENDIENTE, latest.status)
        assertEquals(null, latest.response)
    }

    @Test
    fun parentSuggestionsViewModel_blankInputs_rejected() {
        val viewModel = ParentSuggestionsViewModel(suggestionsRepository, testScope)

        val emptySubject = viewModel.sendSuggestion(
            category = SuggestionCategory.GENERAL,
            subject = "",
            content = "Algún contenido"
        )
        assertFalse(emptySubject)

        val emptyContent = viewModel.sendSuggestion(
            category = SuggestionCategory.GENERAL,
            subject = "Asunto",
            content = "   "
        )
        assertFalse(emptyContent)
    }
}
