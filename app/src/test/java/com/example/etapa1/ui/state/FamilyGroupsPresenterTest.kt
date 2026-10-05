package com.example.etapa1.ui.state

import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.FamilyGroupMessage
import com.example.etapa1.model.FamilyGroupsSampleData
import com.example.etapa1.model.GroupViewer
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyGroupsPresenterTest {

    private val educadora = GroupViewer.Educadora(FamilyGroupsSampleData.EDUCADORA)
    private val familiaGarcia = GroupViewer.Familia(
        participant = FamilyGroupsSampleData.FAMILIA_GARCIA,
        childIds = MockDataRepository.parentChildren.map { it.id }
    )
    private val childrenById = (MockDataRepository.childrenSala1A + MockDataRepository.parentChildren)
        .associateBy { it.id }

    private val grupoConMateo = FamilyGroupChat(
        id = "g1",
        title = "Sofía y Mateo",
        memberChildIds = listOf("sofia_lopez", "mateo_garcia"),
        createdById = FamilyGroupsSampleData.EDUCADORA.id,
        messages = listOf(
            FamilyGroupMessage("m1", FamilyGroupsSampleData.EDUCADORA.id, "Educadora Sala 1A", "Hola", "09:00 AM"),
            FamilyGroupMessage("m2", FamilyGroupsSampleData.FAMILIA_GARCIA.id, "María López (Mamá de Mateo)", "¡Hola!", "09:05 AM")
        )
    )
    private val grupoSinMateo = FamilyGroupChat(
        id = "g2",
        title = "Lucas y Valentina",
        memberChildIds = listOf("lucas_martinez", "valentina_ruiz"),
        createdById = FamilyGroupsSampleData.EDUCADORA.id
    )

    @Test
    fun educadora_seesAllGroups_familySeesOnlyGroupsWithTheirChildren() {
        val groups = listOf(grupoConMateo, grupoSinMateo)

        assertEquals(listOf("g1", "g2"), FamilyGroupsPresenter.groupsVisibleTo(groups, educadora).map { it.id })
        assertEquals(listOf("g1"), FamilyGroupsPresenter.groupsVisibleTo(groups, familiaGarcia).map { it.id })
    }

    @Test
    fun onlyEducadora_canCreateGroups() {
        assertTrue(FamilyGroupsPresenter.canCreateGroups(educadora))
        assertFalse(FamilyGroupsPresenter.canCreateGroups(familiaGarcia))
    }

    @Test
    fun toChat_marksOwnMessagesAsOutgoingDependingOnWhoIsViewing() {
        val asEducadora = FamilyGroupsPresenter.toChat(grupoConMateo, educadora, childrenById)
        val asFamilia = FamilyGroupsPresenter.toChat(grupoConMateo, familiaGarcia, childrenById)

        assertEquals(listOf(true, false), asEducadora.messages.map { it.isOutgoing })
        assertEquals(listOf(false, true), asFamilia.messages.map { it.isOutgoing })
    }

    @Test
    fun toChat_buildsGroupSummary() {
        val chat = FamilyGroupsPresenter.toChat(grupoConMateo, educadora, childrenById)

        assertTrue(chat.isGroup)
        assertEquals("2 familias • Sofía, Mateo", chat.subtitle)
        assertEquals(3, chat.membersCount)
        assertEquals("María López: ¡Hola!", chat.lastMessage)
        assertEquals("09:05 AM", chat.lastMessageTime)
    }

    @Test
    fun previewOf_showsYouForOwnMessagesAndLabelsAttachments() {
        val viewerId = FamilyGroupsSampleData.FAMILIA_GARCIA.id
        val photo = FamilyGroupMessage(
            "m3", viewerId, "María López (Mamá de Mateo)", "", "10:00 AM",
            MessageAttachment("content://foto", "foto.jpg", "image/jpeg")
        )
        val pdf = FamilyGroupMessage(
            "m4", "familia_sofia_lopez", "Marcos Rossi (Papá de Sofía)", "", "10:01 AM",
            MessageAttachment("content://doc", "permiso.pdf", "application/pdf")
        )

        assertEquals("Tú: 📷 Foto", FamilyGroupsPresenter.previewOf(photo, viewerId))
        assertEquals("Marcos Rossi: 📎 permiso.pdf", FamilyGroupsPresenter.previewOf(pdf, viewerId))
    }

    @Test
    fun toChatMessage_keepsAttachmentData() {
        val message = FamilyGroupMessage(
            "m5", "x", "Alguien", "Mira", "10:00 AM",
            MessageAttachment("content://foto", "foto.jpg", "image/jpeg")
        )

        val chatMessage = FamilyGroupsPresenter.toChatMessage(message, viewerId = "otro")

        assertEquals("content://foto", chatMessage.fileUri)
        assertEquals("foto.jpg", chatMessage.fileName)
        assertEquals("image/jpeg", chatMessage.fileMimeType)
        assertFalse(chatMessage.isOutgoing)
    }

    @Test
    fun candidatesFrom_listsEveryFamilyOfTheRoomWithParentName() {
        val candidates = FamilyGroupsPresenter.candidatesFrom(MockDataRepository.getInitialParentChats())

        assertEquals(MockDataRepository.getInitialParentChats().size, candidates.size)
        val mateo = candidates.first { it.childId == "mateo_garcia" }
        assertEquals("Mateo García", mateo.childName)
        assertEquals("María López (Mamá)", mateo.parentName)
    }

    @Test
    fun filterByQuery_matchesTitleChildrenOrLastMessage() {
        val chats = listOf(grupoConMateo, grupoSinMateo).map { FamilyGroupsPresenter.toChat(it, educadora, childrenById) }

        assertEquals(2, FamilyGroupsPresenter.filterByQuery(chats, "  ").size)
        assertEquals(listOf("g1"), FamilyGroupsPresenter.filterByQuery(chats, "mateo").map { it.id })
        assertEquals(listOf("g2"), FamilyGroupsPresenter.filterByQuery(chats, "valentina").map { it.id })
    }
}
