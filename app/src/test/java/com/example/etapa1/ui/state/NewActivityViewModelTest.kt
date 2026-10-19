package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.GroupParticipant
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.components.parseGroupActivityMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewActivityViewModelTest {

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
}
