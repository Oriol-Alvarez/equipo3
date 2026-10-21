package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.GroupParticipant
import com.example.etapa1.model.MealType
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import com.example.etapa1.model.WeeklySummary
import com.example.etapa1.ui.components.parseGroupActivityMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewActivityViewModelTest {

    // saveActivity programa una recarga con viewModelScope (hilo principal de Android)
    @Before
    fun setUpMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDownMain() {
        Dispatchers.resetMain()
    }


    private class FakeBitacoraRepository : BitacoraRepository {
        val activitiesAdded = mutableListOf<Triple<String, String, String>>()
        var lastChildId: String? = null

        override val childIdActual: StateFlow<String?> = MutableStateFlow(null)
        override val timeline: StateFlow<List<TimelineItem>> = MutableStateFlow(emptyList())

        override fun abrir(childId: String) {}

        override fun addChatMessage(
            childId: String,
            text: String,
            isOutgoing: Boolean,
            fileUri: String?,
            fileName: String?,
            fileMimeType: String?,
            senderRole: UserRole?,
            date: String?
        ) {}

        override fun addEventChip(childId: String, time: String, text: String, iconType: String, date: String?) {}

        override fun addActivityCard(
            childId: String,
            time: String,
            category: String,
            portionLabel: String,
            description: String,
            date: String?
        ) {
            activitiesAdded.add(Triple(category, portionLabel, description))
            lastChildId = childId
        }
    }

    private class FakeFamilyGroupsRepository : FamilyGroupsRepository {
        val sentMessages = mutableListOf<Pair<String, String>>()
        val testGroup = FamilyGroupChat(
            id = "fgroup_1",
            title = "Grupo Sala 1A",
            memberChildIds = listOf("mateo_garcia"),
            createdById = "educadora",
            avatarBgColor = 0xFF000000
        )
        override val groups: StateFlow<List<FamilyGroupChat>> = MutableStateFlow(listOf(testGroup))

        override fun currentParticipant(): GroupParticipant = GroupParticipant(
            id = "educadora_1",
            displayName = "Laura"
        )

        override fun createGroup(
            title: String,
            memberChildIds: List<String>,
            creator: GroupParticipant
        ): Result<FamilyGroupChat> = Result.success(testGroup)

        override fun sendMessage(
            groupId: String,
            sender: GroupParticipant,
            text: String,
            attachment: MessageAttachment?
        ): Boolean {
            sentMessages.add(Pair(groupId, text))
            return true
        }
    }

    @Test
    fun saveActivity_callsBitacoraRepository() {
        val bitacoraRepo = FakeBitacoraRepository()
        val viewModel = NewActivityViewModel(bitacoraRepo)

        viewModel.saveActivity(
            time = "10:00 AM",
            category = "Alimentación",
            portionLabel = "TODO",
            description = "Comió toda su fruta",
            childId = "mateo_garcia",
            shareWithFamilyGroups = false
        )

        assertEquals(1, bitacoraRepo.activitiesAdded.size)
        assertEquals("Alimentación", bitacoraRepo.activitiesAdded[0].first)
        assertEquals("TODO", bitacoraRepo.activitiesAdded[0].second)
        assertEquals("Comió toda su fruta", bitacoraRepo.activitiesAdded[0].third)
        assertEquals("mateo_garcia", bitacoraRepo.lastChildId)
    }

    @Test
    fun saveActivity_withShareWithFamilyGroups_sendsToGroup() {
        val bitacoraRepo = FakeBitacoraRepository()
        val familyGroupsRepo = FakeFamilyGroupsRepository()
        val viewModel = NewActivityViewModel(bitacoraRepo, familyGroupsRepo)

        viewModel.saveActivity(
            time = "11:30 AM",
            category = "Descanso",
            portionLabel = "60 min",
            description = "Durmió tranquilo",
            childId = "mateo_garcia",
            shareWithFamilyGroups = true
        )

        assertEquals(1, bitacoraRepo.activitiesAdded.size)
        assertEquals(1, familyGroupsRepo.sentMessages.size)
        assertEquals("fgroup_1", familyGroupsRepo.sentMessages[0].first)
        assertTrue(familyGroupsRepo.sentMessages[0].second.contains("[ACTIVIDAD]"))
        assertTrue(familyGroupsRepo.sentMessages[0].second.contains("Descanso"))
    }

    @Test
    fun parseGroupActivityMessage_parsesCorrectly() {
        val raw = "[ACTIVIDAD]\nCategoría: Alimentación\nDetalle: Consumo: TODO\nObservaciones: Disfrutó mucho la comida"
        val parsed = parseGroupActivityMessage(raw)

        assertNotNull(parsed)
        assertEquals("Alimentación", parsed?.first)
        assertEquals("Consumo: TODO", parsed?.second)
        assertEquals("Disfrutó mucho la comida", parsed?.third)
    }

    @Test
    fun parseGroupActivityMessage_returnsNullForRegularMessage() {
        val raw = "Hola a todos, bienvenidos al grupo"
        val parsed = parseGroupActivityMessage(raw)

        assertNull(parsed)
    }

    @Test
    fun formatActivityPreview_formatsActivityCorrectly() {
        val raw = "[ACTIVIDAD]\nCategoría: Alimentación\nDetalle: Desayuno: Comió todo\nObservaciones: Le gustó el puré"
        val preview = com.example.etapa1.ui.components.formatActivityPreview(raw)
        assertEquals("📋 Actividad: Alimentación • Desayuno: Comió todo", preview)

        val rawWithYo = "Yo: [ACTIVIDAD]\nCategoría: Descanso\nDetalle: 60 min\nObservaciones: Durmió bien"
        val previewWithYo = com.example.etapa1.ui.components.formatActivityPreview(rawWithYo)
        assertEquals("Yo: 📋 Actividad: Descanso • 60 min", previewWithYo)
    }

    @Test
    fun formatActivityDescription_cleansDuplicatedBadgePrefix() {
        val badge = "Desayuno: Comió todo"
        val descWithRepeat = "Desayuno: Comió todo. Observaciones: Le gustó el puré"
        val cleaned = com.example.etapa1.ui.components.formatActivityDescription(descWithRepeat, badge)
        assertEquals("Observaciones: Le gustó el puré", cleaned)

        val descNormal = "El alumno participó activamente"
        val cleanedNormal = com.example.etapa1.ui.components.formatActivityDescription(descNormal, badge)
        assertEquals("El alumno participó activamente", cleanedNormal)
    }

    @Test
    fun saveActivity_callsChildRepositoryCargarDetalle() {
        val bitacoraRepo = FakeBitacoraRepository()
        var detalleCargado: String? = null
        val fakeChildRepo = object : ChildRepository {
            override val childrenOfRoom = MutableStateFlow<List<Child>>(emptyList())
            override val parentChildren = MutableStateFlow<List<Child>>(emptyList())
            override val salaActual = MutableStateFlow("sala_1a")
            override val cambios = MutableStateFlow(0)
            override fun abrirSala(roomId: String) {}
            override fun cargarDetalle(childId: String) { detalleCargado = childId }
            override fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String) {}
            override fun refresh() {}
            override fun getChildById(childId: String): Child? = null
            override fun getChildFullProfile(child: Child): ChildFullProfile = MockDataRepository.getChildFullProfile(child)
            override fun updateChildFullProfile(childId: String, profile: ChildFullProfile) {}
            override fun getWeeklySummaryForChild(child: Child): WeeklySummary = MockDataRepository.getWeeklySummaryForChild(child)
            override fun getWeeklySummariesForChild(child: Child): List<WeeklySummary> = emptyList()
            override fun markWeeklySummaryReadStatus(childId: String, weekRangeText: String, isRead: Boolean, timestampText: String?) {}
            override fun setWeeklySummaryReaction(childId: String, weekRangeText: String, reaction: String?) {}
            override fun getDailyBitacorasForChild(child: Child): List<DailyBitacora> = emptyList()
        }

        val viewModel = NewActivityViewModel(bitacoraRepo, childRepository = fakeChildRepo)
        viewModel.saveActivity(
            time = "12:30 PM",
            category = ActivityCategory.ALIMENTACION,
            summaryBadge = "Comió todo",
            description = "Comió todo su plato",
            childId = "mateo_garcia",
            mealType = MealType.COMIDA,
            foodConsumption = FoodConsumptionOption.TODO
        )

        assertEquals("mateo_garcia", detalleCargado)
    }

    @Test
    fun saveActivity_withEstadoAnimo_updatesDailyLogEstadiaMood() {
        val bitacoraRepo = FakeBitacoraRepository()
        var updatedEstadia: com.example.etapa1.model.EstadiaLog? = null
        val fakeDailyLogRepo = object : com.example.etapa1.data.repository.DailyLogRepository {
            override val logs = MutableStateFlow<List<com.example.etapa1.model.DailyLog>>(emptyList())
            override fun logFor(childId: String, date: com.example.etapa1.domain.SimpleDate) = null
            override fun logsFor(childId: String, days: List<com.example.etapa1.domain.SimpleDate>) = emptyList<com.example.etapa1.model.DailyLog?>()
            override fun saveIngreso(childId: String, date: com.example.etapa1.domain.SimpleDate, ingreso: com.example.etapa1.model.IngresoLog) = Result.failure<com.example.etapa1.model.DailyLog>(NotImplementedError())
            override fun saveEstadia(childId: String, date: com.example.etapa1.domain.SimpleDate, estadia: com.example.etapa1.model.EstadiaLog) = Result.failure<com.example.etapa1.model.DailyLog>(NotImplementedError())
            override fun updateEstadia(childId: String, date: com.example.etapa1.domain.SimpleDate, change: (com.example.etapa1.model.EstadiaLog) -> com.example.etapa1.model.EstadiaLog): Result<com.example.etapa1.model.DailyLog> {
                val initial = com.example.etapa1.model.EstadiaLog()
                val updated = change(initial)
                updatedEstadia = updated
                val dummyLog = com.example.etapa1.model.DailyLog(childId = childId, date = date, ingreso = null, estadia = updated, egreso = null)
                return Result.success(dummyLog)
            }
            override fun saveEgreso(childId: String, date: com.example.etapa1.domain.SimpleDate, egreso: com.example.etapa1.model.EgresoLog) = Result.failure<com.example.etapa1.model.DailyLog>(NotImplementedError())
            override fun recordAttendanceTime(childId: String, date: com.example.etapa1.domain.SimpleDate, isArrival: Boolean, time24h: String, recordedBy: String) = Result.failure<com.example.etapa1.model.DailyLog>(NotImplementedError())
        }

        val viewModel = NewActivityViewModel(bitacoraRepo, dailyLogRepository = fakeDailyLogRepo)
        viewModel.saveActivity(
            time = "10:30 AM",
            category = ActivityCategory.ESTADO_ANIMO,
            summaryBadge = "😊 Feliz",
            description = "Estadía: Feliz. En actividades: ¿Lloró?: No, ¿Peleó?: No, ¿Participó?: Sí.",
            childId = "mateo_garcia",
            moodState = com.example.etapa1.model.MoodState.FELIZ
        )

        assertNotNull(updatedEstadia)
        assertEquals(com.example.etapa1.model.MoodState.FELIZ, updatedEstadia?.mood)
        assertEquals(true, updatedEstadia?.participated)
        assertEquals(false, updatedEstadia?.cried)
    }
}
