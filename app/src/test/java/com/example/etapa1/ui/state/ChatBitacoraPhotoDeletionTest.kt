package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockFamilyGroupsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatBitacoraPhotoDeletionTest {

    private lateinit var bitacoraRepo: MockBitacoraRepositoryImpl
    private lateinit var chatBitacoraVm: ChatBitacoraViewModel

    private lateinit var familyGroupsRepo: MockFamilyGroupsRepositoryImpl
    private lateinit var familyGroupsVm: FamilyGroupsViewModel

    @Before
    fun setUp() {
        bitacoraRepo = MockBitacoraRepositoryImpl()
        chatBitacoraVm = ChatBitacoraViewModel(
            bitacoraRepository = bitacoraRepo
        )

        familyGroupsRepo = MockFamilyGroupsRepositoryImpl()
        val childRepo = MockChildRepositoryImpl()
        val messagesRepo = MockMessagesRepositoryImpl()
        familyGroupsVm = FamilyGroupsViewModel(
            familyGroupsRepository = familyGroupsRepo,
            messagesRepository = messagesRepo,
            childRepository = childRepo,
            bitacoraRepository = bitacoraRepo
        )
    }

    @Test
    fun deletePhoto_inBitacoraChat_withText_keepsTextAndRemovesPhoto() = runTest {
        chatBitacoraVm.abrir("mateo_garcia")

        // 1. Enviar mensaje con foto y texto
        chatBitacoraVm.sendMessage(
            text = "Mateo comiendo fruta",
            fileUri = "content://media/external/images/media/10",
            fileName = "fruta.jpg",
            fileMimeType = "image/jpeg",
            senderRole = UserRole.FAMILIAR,
            childId = "mateo_garcia"
        )

        val timeline = bitacoraRepo.timeline.value
        val photoMsg = timeline.filterIsInstance<TimelineItem.ChatMessage>().last()
        assertEquals("Mateo comiendo fruta", photoMsg.message)
        assertEquals("content://media/external/images/media/10", photoMsg.fileUri)

        // 2. Eliminar la foto
        chatBitacoraVm.deletePhoto(photoMsg.id, "mateo_garcia")

        // 3. Comprobar que el texto sigue pero la foto es null
        val updatedTimeline = bitacoraRepo.timeline.value
        val updatedMsg = updatedTimeline.filterIsInstance<TimelineItem.ChatMessage>().find { it.id == photoMsg.id }
        assertNotNull(updatedMsg)
        assertEquals("Mateo comiendo fruta", updatedMsg?.message)
        assertNull(updatedMsg?.fileUri)
        assertNull(updatedMsg?.fileName)
        assertNull(updatedMsg?.fileMimeType)
    }

    @Test
    fun deletePhoto_inBitacoraChat_withoutText_removesMessageCompletely() = runTest {
        chatBitacoraVm.abrir("mateo_garcia")

        // 1. Enviar solo foto sin texto
        chatBitacoraVm.sendMessage(
            text = "",
            fileUri = "content://media/external/images/media/20",
            fileName = "siesta.jpg",
            fileMimeType = "image/jpeg",
            senderRole = UserRole.FAMILIAR,
            childId = "mateo_garcia"
        )

        val timeline = bitacoraRepo.timeline.value
        val photoMsg = timeline.filterIsInstance<TimelineItem.ChatMessage>().last()
        assertEquals("content://media/external/images/media/20", photoMsg.fileUri)

        // 2. Eliminar la foto
        chatBitacoraVm.deletePhoto(photoMsg.id, "mateo_garcia")

        // 3. Comprobar que el mensaje completo fue removido de la línea de tiempo
        val updatedTimeline = bitacoraRepo.timeline.value
        val exists = updatedTimeline.filterIsInstance<TimelineItem.ChatMessage>().any { it.id == photoMsg.id }
        assertTrue(!exists)
    }

    @Test
    fun deletePhoto_inFamilyGroup_viaViewModel() = runTest {
        val group = familyGroupsRepo.groups.value.first()
        val photo = MessageAttachment("content://media/30", "paseo.jpg", "image/jpeg")
        familyGroupsRepo.sendMessage(group.id, com.example.etapa1.model.FamilyGroupsSampleData.FAMILIA_GARCIA, "Foto en el parque", photo)

        val msgId = familyGroupsRepo.groups.value.first { it.id == group.id }.messages.last().id

        // Eliminar foto vía FamilyGroupsViewModel
        familyGroupsVm.deletePhoto(group.id, msgId)

        val updatedGroup = familyGroupsRepo.groups.value.first { it.id == group.id }
        val msg = updatedGroup.messages.find { it.id == msgId }
        assertNotNull(msg)
        assertEquals("Foto en el parque", msg?.text)
        assertNull(msg?.attachment)
    }
}
