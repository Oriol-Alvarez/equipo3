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
        assertTrue(viewModel.uiState.value.weeklySummaries.size >= 4)
        assertEquals("Semana actual", viewModel.uiState.value.weeklySummaries[0].weekLabel)
        assertEquals("15 - 19 Sep", viewModel.uiState.value.weeklySummaries[1].weekLabel)

        viewModel.selectChild(sibling)

        assertEquals("Lucía García López", viewModel.uiState.value.selectedChild.fullName)
        assertEquals("Lucía García López", viewModel.uiState.value.fullProfile.child.fullName)
        assertTrue(viewModel.uiState.value.weeklySummaries.size >= 4)
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

    @Test
    fun parentHomeViewModel_updateChildProfile_updatesStateAndRepository() {
        val child = MockDataRepository.mateoGarcia
        val viewModel = ParentHomeViewModel(childRepository, child)

        val newPickups = listOf("Laura García (Tía) - DNI: 12345678A", "Manuel García (Abuelo) - DNI: 87654321B")
        viewModel.updateChildProfile(
            pediatrician = "Dr. Carlos Valdés",
            pediatricianPhone = "+34 600 999 888",
            medicalNotes = "Control de intolerancia a la lactosa superado. Sin medicación actual.",
            habitsAndPedagogicalNotes = "Le gusta dormir con música suave. Muy autónomo comiendo.",
            emergencyPhone = "+34 611 777 555",
            authorizedPickups = newPickups,
            medicalCertificateUri = "content://media/external/file/101",
            medicalCertificateName = "Certificado_Pediatrico_2026.pdf",
            medicalCertificateDate = "Certificado el 30/09/2026"
        )

        // Verificar StateFlow de ViewModel
        val updatedProfile = viewModel.uiState.value.fullProfile
        assertEquals("Dr. Carlos Valdés", updatedProfile.pediatrician)
        assertEquals("+34 600 999 888", updatedProfile.pediatricianPhone)
        assertEquals("Control de intolerancia a la lactosa superado. Sin medicación actual.", updatedProfile.medicalNotes)
        assertEquals("Le gusta dormir con música suave. Muy autónomo comiendo.", updatedProfile.generalNotes)
        assertEquals("+34 611 777 555", updatedProfile.emergencyPhone)
        assertEquals(2, updatedProfile.authorizedPickups.size)
        assertEquals("Laura García (Tía) - DNI: 12345678A", updatedProfile.authorizedPickups[0])
        assertEquals("content://media/external/file/101", updatedProfile.medicalCertificateUri)
        assertEquals("Certificado_Pediatrico_2026.pdf", updatedProfile.medicalCertificateName)
        assertEquals("Certificado el 30/09/2026", updatedProfile.medicalCertificateDate)

        // Verificar persistencia en el repositorio
        val repoProfile = childRepository.getChildFullProfile(child)
        assertEquals("Dr. Carlos Valdés", repoProfile.pediatrician)
        assertEquals("Certificado_Pediatrico_2026.pdf", repoProfile.medicalCertificateName)
        assertEquals(newPickups, repoProfile.authorizedPickups)
    }
}
