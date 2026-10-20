package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockFamilyGroupsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IndividualAndSearchChatsTest {

    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testEducatorMappingForMateoAndLucia() {
        val mateoEducator = MockDataRepository.getEducatorForChild("mateo_garcia")
        assertNotNull(mateoEducator)
        assertEquals("Laura Gómez", mateoEducator?.name)
        assertEquals("Educadora Titular • Sala 1A", mateoEducator?.role)

        val luciaEducator = MockDataRepository.getEducatorForChild("lucia_garcia")
        assertNotNull(luciaEducator)
        assertEquals("Carlos Ruiz", luciaEducator?.name)
        assertEquals("Educador Lactantes", luciaEducator?.role)

        assertNotEquals(mateoEducator?.id, luciaEducator?.id)
    }

    @Test
    fun testIsolatedTimelinesForMateoAndLucia() {
        // La bitácora es de un niño a la vez: lo que se agrega a Lucía no aparece en la de Mateo
        val repo = MockBitacoraRepositoryImpl()
        repo.abrir("mateo_garcia")
        val mateoInicial = repo.timeline.value
        assertTrue("Mateo debe tener eventos iniciales", mateoInicial.isNotEmpty())

        repo.abrir("lucia_garcia")
        repo.addChatMessage(
            childId = "lucia_garcia",
            text = "Lucía ya tomó su puré de verduras",
            isOutgoing = false
        )
        val updatedLucia = repo.timeline.value
        assertTrue(updatedLucia.any { it is TimelineItem.ChatMessage && it.message == "Lucía ya tomó su puré de verduras" })

        repo.abrir("mateo_garcia")
        val updatedMateo = repo.timeline.value
        assertEquals(mateoInicial.size, updatedMateo.size)
        assertFalse(updatedMateo.any { it is TimelineItem.ChatMessage && it.message == "Lucía ya tomó su puré de verduras" })
    }

    @Test
    fun testSearchAndStartNewStaffChatFromFamilyRole() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(false)

        // Buscar a Sara Navarro (Psicopedagoga), que no está en los chats iniciales de la familia
        viewModel.toggleSearch()
        viewModel.onSearchQueryChanged("Sara")

        val state = viewModel.uiState.value
        assertEquals("Sara", state.searchQuery)
        assertTrue(state.isSearchOpen)
        assertTrue("Sara Navarro debe aparecer como candidata disponible", state.availableStaffToChat.any { it.name.contains("Sara", ignoreCase = true) })

        val sara = state.availableStaffToChat.first { it.name.contains("Sara", ignoreCase = true) }
        val newChat = viewModel.startNewFamilyEducatorChat(sara)
        assertEquals("Sara Navarro", newChat.title)

        // Al crear el chat, se agrega a los chats de familia y ya no aparece en disponibles
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.familyChats.any { it.title == "Sara Navarro" })
        assertFalse(updatedState.availableStaffToChat.any { it.name.contains("Sara", ignoreCase = true) })
    }

    @Test
    fun testSearchAndStartNewStaffChatFromWorkerRole() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(true)

        // Buscar a Javier Ortiz (Mantenimiento)
        viewModel.toggleSearch()
        viewModel.onSearchQueryChanged("Javier")

        val state = viewModel.uiState.value
        assertTrue("Javier Ortiz debe aparecer en disponibles para el personal", state.availableStaffToChat.any { it.name.contains("Javier", ignoreCase = true) })

        val javier = state.availableStaffToChat.first { it.name.contains("Javier", ignoreCase = true) }
        val newChat = viewModel.startNewWorkerChat(javier)
        assertEquals("Javier Ortiz", newChat.title)

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.workerChats.any { it.title == "Javier Ortiz" })
        assertFalse(updatedState.availableStaffToChat.any { it.name.contains("Javier", ignoreCase = true) })
    }

    @Test
    fun testSearchAndStartNewChildChatFromEducatorRole() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(true)

        val child = Child(
            id = "valeria_test",
            shortName = "Valeria T.",
            fullName = "Valeria Test",
            roomText = "Sala 1A",
            groupText = "Grupo 1A",
            statusText = "08:30 AM",
            ageText = "2 años",
            avatarBgColor = 0xFF4CAF50,
            avatarInitials = "VT",
            isPresent = true
        )
        val newChatSummary = viewModel.startNewParentChat(child)
        assertEquals("Valeria Test", newChatSummary.child.fullName)

        val state = viewModel.uiState.value
        assertTrue(state.parentChats.any { it.child.id == "valeria_test" })
    }

    @Test
    fun testEducatorToFamiliarMessageVisibilityDirection() {
        val educatorMsg = TimelineItem.ChatMessage(
            id = "test_msg_1",
            time = "10:00 AM",
            date = "12/10/2026",
            message = "Hola familia, Mateo durmió muy bien.",
            isOutgoing = true,
            senderRole = com.example.etapa1.model.UserRole.EDUCADORA
        )

        // Para la educadora (isWorkerRole = true), este mensaje es saliente (isOutgoing = true)
        val forWorker = if (educatorMsg.senderRole != null) {
            educatorMsg.senderRole == com.example.etapa1.model.UserRole.EDUCADORA
        } else educatorMsg.isOutgoing
        assertTrue("Para la educadora el mensaje debe ser saliente", forWorker)

        // Para el familiar (isWorkerRole = false), este mensaje es entrante (isOutgoing = false)
        val forFamily = if (educatorMsg.senderRole != null) {
            educatorMsg.senderRole == com.example.etapa1.model.UserRole.FAMILIAR
        } else !educatorMsg.isOutgoing
        assertFalse("Para el familiar el mensaje de la educadora debe ser entrante para mostrarse con fondo blanco y texto oscuro", forFamily)
    }

    @Test
    fun testMateoChatVisibleInEducatorParentChats() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(true)

        val state = viewModel.uiState.value
        assertTrue("El rol debe ser trabajador / educadora", state.isWorkerRole)
        val mateoChat = state.parentChats.find { it.child.id == MockDataRepository.mateoGarcia.id }
        assertNotNull("El chat con Mateo debe existir en la lista de chats de padres para la educadora", mateoChat)
        assertEquals("Mateo García", mateoChat?.child?.fullName)
        assertEquals("María López (Mamá)", mateoChat?.parentName)
    }

    @Test
    fun testSearchMateoFromEducatorRoleReturnsMateoChat() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(true)
        viewModel.toggleSearch()
        viewModel.onSearchQueryChanged("Mateo")

        val state = viewModel.uiState.value
        assertEquals("Mateo", state.searchQuery)
        assertTrue(state.isSearchOpen)
        assertTrue(state.parentChats.isNotEmpty())
        assertTrue("El chat filtrado debe ser el de Mateo García", state.parentChats.all { it.child.fullName.contains("Mateo", ignoreCase = true) })
        assertEquals("Mateo García", state.parentChats.first().child.fullName)
    }

    @Test
    fun testRecargarMessagesViewModel() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.recargar()
        val mateoChat = viewModel.uiState.value.parentChats.find { it.child.id == MockDataRepository.mateoGarcia.id }
        assertNotNull("Tras recargar, el chat de Mateo debe estar disponible", mateoChat)
    }

    @Test
    fun testFamiliarUnreadCountDoesNotIncludeParentChats() {
        val repo = MockMessagesRepositoryImpl()
        val viewModel = MessagesViewModel(
            messagesRepository = repo,
            coroutineScope = testScope
        )
        viewModel.setWorkerRole(false)

        val state = viewModel.uiState.value
        assertFalse("El rol debe ser familiar", state.isWorkerRole)
        assertEquals("Total de unread en parents debe ser 0 para familiar", 0, state.totalParentsUnreadCount)

        // Marcar como leído el chat de educadora
        viewModel.markWorkerChatAsRead("fchat_1")
        val updatedState = viewModel.uiState.value
        assertEquals("Unread para familiar debe ser 0 tras leer", 0, updatedState.totalWorkersUnreadCount)
    }

    @Test
    fun testFamilyGroupSendActivitySavesToChildrenBitacora() {
        val familyGroupsRepo = MockFamilyGroupsRepositoryImpl()
        val messagesRepo = MockMessagesRepositoryImpl()
        val childRepo = MockChildRepositoryImpl()
        val bitacoraRepo = MockBitacoraRepositoryImpl()

        val viewModel = FamilyGroupsViewModel(
            familyGroupsRepository = familyGroupsRepo,
            messagesRepository = messagesRepo,
            childRepository = childRepo,
            bitacoraRepository = bitacoraRepo,
            coroutineScope = testScope
        )
        viewModel.useEducatorViewer()

        val groups = familyGroupsRepo.groups.value
        assertTrue("Debe existir al menos un grupo familiar", groups.isNotEmpty())
        val targetGroup = groups.first()

        viewModel.sendGroupActivity(
            groupId = targetGroup.id,
            category = "Pedagógica",
            badge = "Pintura de dedos",
            description = "Taller grupal con témperas no tóxicas"
        )

        // Verificar que el mensaje se envió al grupo
        val updatedGroup = familyGroupsRepo.groups.value.first { it.id == targetGroup.id }
        assertTrue("El chat del grupo debe contener la actividad", updatedGroup.messages.any { it.text.contains("[ACTIVIDAD]") })

        // Verificar que se guardó en la bitácora de cada niño del grupo
        val targetChildren = targetGroup.memberChildIds.ifEmpty { listOf("mateo_garcia") }
        for (childId in targetChildren) {
            bitacoraRepo.abrir(childId)
            val timeline = bitacoraRepo.timeline.value
            val hasActivity = timeline.filterIsInstance<TimelineItem.ActivityCard>().any {
                it.description.contains("Taller grupal con témperas no tóxicas")
            }
            assertTrue("La actividad grupal debe haberse guardado en la bitácora de $childId", hasActivity)
        }
    }

    @Test
    fun testActiveChildEducatorFilteringForFamiliar() {
        val mateo = MockDataRepository.mateoGarcia
        val lucia = MockDataRepository.luciaGarcia

        val mateoEducator = MockDataRepository.getEducatorForChild(mateo.id)
        val luciaEducator = MockDataRepository.getEducatorForChild(lucia.id)

        assertEquals("Laura Gómez", mateoEducator.name)
        assertEquals("Carlos Ruiz", luciaEducator.name)

        // Simular lista de chats con personal para familiar
        val initialChats = MockDataRepository.getInitialFamilyChats()
        assertTrue(initialChats.any { it.title.equals("Laura Gómez", ignoreCase = true) })
        assertTrue(initialChats.any { it.title.equals("Carlos Ruiz", ignoreCase = true) })

        // Filtro cuando el hijo activo es Mateo: Carlos Ruiz (educador de Lucía) debe ser excluido
        val otherForMateo = MockDataRepository.parentChildren.filter { it.id != mateo.id }
            .map { MockDataRepository.getEducatorForChild(it.id).name.lowercase() }.toSet()
        val visibleForMateo = initialChats.filter { !otherForMateo.contains(it.title.trim().lowercase()) }

        assertTrue(visibleForMateo.any { it.title == "Laura Gómez" })
        assertFalse("Carlos Ruiz no debe aparecer cuando Mateo es el hijo activo",
            visibleForMateo.any { it.title == "Carlos Ruiz" })

        // Filtro cuando la hija activa es Lucía: Laura Gómez (educadora de Mateo) debe ser excluida
        val otherForLucia = MockDataRepository.parentChildren.filter { it.id != lucia.id }
            .map { MockDataRepository.getEducatorForChild(it.id).name.lowercase() }.toSet()
        val visibleForLucia = initialChats.filter { !otherForLucia.contains(it.title.trim().lowercase()) }

        assertTrue(visibleForLucia.any { it.title == "Carlos Ruiz" })
        assertFalse("Laura Gómez no debe aparecer cuando Lucía es la hija activa",
            visibleForLucia.any { it.title == "Laura Gómez" })
    }

    @Test
    fun testFamilyGroupsFilteredByActiveChild() {
        val familyGroupsRepo = MockFamilyGroupsRepositoryImpl()
        val messagesRepo = MockMessagesRepositoryImpl()
        val childRepo = MockChildRepositoryImpl()

        val viewModel = FamilyGroupsViewModel(
            familyGroupsRepository = familyGroupsRepo,
            messagesRepository = messagesRepo,
            childRepository = childRepo,
            coroutineScope = testScope
        )

        // 1. Ver grupos de Mateo
        viewModel.useFamilyViewer(listOf("mateo_garcia"))
        val mateoGroups = viewModel.uiState.value.groups
        for (g in mateoGroups) {
            val origin = familyGroupsRepo.groups.value.first { it.id == g.id }
            assertTrue("El grupo ${g.title} debe incluir a Mateo", origin.memberChildIds.contains("mateo_garcia"))
        }

        // 2. Ver grupos de Lucía
        viewModel.useFamilyViewer(listOf("lucia_garcia"))
        val luciaGroups = viewModel.uiState.value.groups
        for (g in luciaGroups) {
            val origin = familyGroupsRepo.groups.value.first { it.id == g.id }
            assertTrue("El grupo ${g.title} debe incluir a Lucía", origin.memberChildIds.contains("lucia_garcia"))
        }
    }

    @Test
    fun testChatBitacoraSendMessageUpdatesMessagesRepository() {
        val bitacoraRepo = MockBitacoraRepositoryImpl()
        val messagesRepo = MockMessagesRepositoryImpl()
        val chatVm = ChatBitacoraViewModel(
            bitacoraRepository = bitacoraRepo,
            messagesRepository = messagesRepo
        )

        chatVm.abrir("mateo_garcia")
        chatVm.sendMessage(
            text = "Mateo tomó toda su merienda y está contento",
            senderRole = com.example.etapa1.model.UserRole.EDUCADORA
        )

        // 1. Debe estar en la bitácora
        val timeline = bitacoraRepo.timeline.value
        assertTrue(timeline.any { it is TimelineItem.ChatMessage && it.message.contains("Mateo tomó toda su merienda") })

        // 2. Debe haber actualizado el resumen de chats en parentChats
        val firstParentChat = messagesRepo.parentChats.value.first()
        assertEquals("mateo_garcia", firstParentChat.child.id)
        assertTrue(firstParentChat.lastMessage.contains("Mateo tomó toda su merienda"))

        // 3. Debe haber actualizado el chat familiar correspondiente (Laura Gómez)
        val firstFamilyChat = messagesRepo.familyChats.value.first()
        assertTrue(firstFamilyChat.title.contains("Laura Gómez"))
        assertTrue(firstFamilyChat.lastMessage.contains("Mateo tomó toda su merienda"))
    }

    @Test
    fun testEducatorDetailsFormatForFamiliarChat() {
        val child = Child(
            id = "mateo_garcia",
            fullName = "Mateo García López",
            shortName = "Mateo",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Maternal A",
            statusText = "Presente",
            isPresent = true,
            allergyAlert = "Alergia a Nuez",
            educatorName = "Laura Gómez",
            educatorRole = "Educadora Titular",
            educatorAvatarInitials = "LG",
            educatorAvatarBgColor = 4278364369L
        )

        val educatorRole = (child.educatorRole?.ifBlank { null } ?: "").substringBefore(" •").trim()
        val roomName = child.roomText.ifBlank { "Sala" }
        val subtitle = "$educatorRole • $roomName"

        assertEquals("Educadora Titular • Sala 1A", subtitle)
        assertTrue("El subtítulo no debe incluir el nombre del alumno", !subtitle.contains("Mateo"))
        assertTrue("El subtítulo no debe incluir 'de '", !subtitle.contains("de "))
    }
}

