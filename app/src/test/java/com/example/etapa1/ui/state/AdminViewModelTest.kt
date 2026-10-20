package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAdminRepositoryImpl
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdminViewModelTest {

    private lateinit var repository: MockAdminRepositoryImpl
    private lateinit var viewModel: AdminViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        repository = MockAdminRepositoryImpl()
        viewModel = AdminViewModel(repository, testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun initialState_hasDefaultTabAndData() {
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedTab)
        assertEquals("TODOS", state.selectedUserFilter)
        assertEquals("TODOS", state.selectedLogFilter)

        val users = viewModel.users.value
        assertTrue("Debe cargar usuarios iniciales", users.isNotEmpty())

        val stats = viewModel.stats.value
        assertTrue("Total niños debe ser positivo", stats.totalChildren > 0)
        assertTrue("Presentes debe ser positivo", stats.presentCount > 0)

        val logs = viewModel.logs.value
        assertTrue("Debe haber registros de log iniciales", logs.isNotEmpty())
    }

    @Test
    fun selectTab_updatesUiState() {
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(4)
        assertEquals(4, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun repository_userCrud_works() = runBlocking {
        val initialCount = repository.users.value.size

        val createRes = repository.createUser(
            email = "roberto@sonrisas.edu",
            password = "Password123!",
            role = "EDUCADORA",
            displayName = "Prof. Roberto Sánchez",
            phone = "+52 555 777 999"
        )
        assertTrue(createRes.isSuccess)
        val created = createRes.getOrNull()!!
        assertEquals("roberto@sonrisas.edu", created.email)
        assertEquals(initialCount + 1, repository.users.value.size)

        val updateRes = repository.updateUser(
            userId = created.id,
            displayName = "Roberto S. Actualizado",
            email = null,
            phone = "+52 555 000 111",
            role = null,
            password = null
        )
        assertTrue(updateRes.isSuccess)
        assertEquals("Roberto S. Actualizado", updateRes.getOrNull()?.displayName)

        val deleteRes = repository.deleteUser(created.id)
        assertTrue(deleteRes.isSuccess)
        assertEquals(initialCount, repository.users.value.size)
    }

    @Test
    fun repository_announcementCrud_works() = runBlocking {
        val initialCount = repository.announcements.value.size

        val createRes = repository.createAnnouncement(
            title = "Suspensión de Clases por Mantenimiento",
            summary = "El viernes no habrá servicio por obras.",
            fullContent = "Estimadas familias: Se informa que el viernes las instalaciones permanecerán cerradas.",
            author = "Dirección del Centro"
        )
        assertTrue(createRes.isSuccess)
        assertEquals(initialCount + 1, repository.announcements.value.size)
        val created = repository.announcements.value.first()
        assertEquals("Suspensión de Clases por Mantenimiento", created.title)

        val updateRes = repository.updateAnnouncement(
            id = created.id,
            title = "Aviso Modificado",
            summary = null,
            fullContent = null,
            author = null
        )
        assertTrue(updateRes.isSuccess)
        assertEquals("Aviso Modificado", repository.announcements.value.first().title)

        val deleteRes = repository.deleteAnnouncement(created.id)
        assertTrue(deleteRes.isSuccess)
        assertEquals(initialCount, repository.announcements.value.size)
    }

    @Test
    fun repository_lostObjectsCrud_works() = runBlocking {
        val item = repository.lostObjects.value.first()
        val initialStatus = item.isClaimed

        val updateRes = repository.updateLostObject(
            id = item.id,
            title = null,
            description = null,
            category = null,
            location = null,
            room = null,
            isClaimed = !initialStatus
        )
        assertTrue(updateRes.isSuccess)
        val updated = repository.lostObjects.value.first { it.id == item.id }
        assertEquals(!initialStatus, updated.isClaimed)
    }

    @Test
    fun repository_suggestionAnswer_works() = runBlocking {
        val sug = repository.suggestions.value.first()

        val answerRes = repository.updateSuggestionStatus(
            id = sug.id,
            status = "ATENDIDA",
            response = "Se ha tomado nota y se incorporará fruta fresca inmediatamente.",
            responderName = "Dirección General"
        )
        assertTrue(answerRes.isSuccess)

        val updated = repository.suggestions.value.first { it.id == sug.id }
        assertEquals(SuggestionStatus.ATENDIDA, updated.status)
        assertEquals("Se ha tomado nota y se incorporará fruta fresca inmediatamente.", updated.response)
    }

    @Test
    fun repository_logsFiltering_works() = runBlocking {
        repository.loadLogs(category = "ASISTENCIA", search = null)
        val filteredLogs = repository.logs.value
        assertTrue("Todos los logs filtrados deben ser de categoría ASISTENCIA", filteredLogs.all { it.category == "ASISTENCIA" })
    }

    @Test
    fun parentUser_hasChildren_andCanLinkAndUnlink() = runBlocking {
        // User usr_familia_garcia starts with mateo_garcia and lucia_garcia
        val parent = repository.users.value.first { it.id == "usr_familia_garcia" }
        assertEquals(2, parent.children.size)

        // Unlink lucia_garcia
        val unlinkRes = repository.unlinkChildFromParent("usr_familia_garcia", "lucia_garcia")
        assertTrue(unlinkRes.isSuccess)
        val parentAfterUnlink = repository.users.value.first { it.id == "usr_familia_garcia" }
        assertEquals(1, parentAfterUnlink.children.size)
        assertEquals("mateo_garcia", parentAfterUnlink.children.first().id)

        // Relink lucia_garcia
        val linkRes = repository.linkChildToParent("usr_familia_garcia", "lucia_garcia")
        assertTrue(linkRes.isSuccess)
        val parentAfterRelink = repository.users.value.first { it.id == "usr_familia_garcia" }
        assertEquals(2, parentAfterRelink.children.size)
    }

    @Test
    fun repository_createAndLinkChild_andUpdateChild() = runBlocking {
        val createRes = repository.createAndLinkChild(
            userId = "usr_familia_garcia",
            fullName = "Alejandro García López",
            room = "Sala 2B",
            bloodType = "A+",
            pediatrician = "Dra. Carmen Soto",
            emergencyPhone = "+52 555 444 333",
            medicalNotes = "Intolerancia a la lactosa"
        )
        assertTrue(createRes.isSuccess)
        val createdChild = createRes.getOrNull()!!
        assertEquals("Alejandro García López", createdChild.fullName)
        assertEquals("Sala 2B", createdChild.roomText)

        val parent = repository.users.value.first { it.id == "usr_familia_garcia" }
        assertTrue(parent.children.any { it.id == createdChild.id })

        val updateRes = repository.updateChild(
            childId = createdChild.id,
            fullName = "Alejandro García Actualizado",
            room = "Sala 2B",
            bloodType = "A+",
            pediatrician = "Dra. Carmen Soto",
            emergencyPhone = "+52 555 999 888",
            medicalNotes = "Intolerancia a la lactosa y al polen",
            allergyAlert = "Lactosa y Polen",
            birthDate = "2023-05-10"
        )
        assertTrue(updateRes.isSuccess)
        val updatedChild = updateRes.getOrNull()!!
        assertEquals("Alejandro García Actualizado", updatedChild.fullName)
        assertEquals("Lactosa y Polen", updatedChild.allergyAlert)
        assertEquals("+52 555 999 888", updatedChild.emergencyPhone)
    }
}

