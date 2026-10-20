package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MessagesRepositoryTest {

    private lateinit var repository: MockMessagesRepositoryImpl

    @Before
    fun setUp() {
        repository = MockMessagesRepositoryImpl()
    }

    @Test
    fun initialParentChat_mateoGarciaHasUnreadMessage() {
        val mateoChat = repository.parentChats.value.firstOrNull { it.child.id == MockDataRepository.mateoGarcia.id }
        assertTrue("Mateo García chat debe existir", mateoChat != null)
        assertEquals(1, mateoChat?.unreadCount)
    }

    @Test
    fun markParentChatAsRead_resetsUnreadCountToZero() {
        val childId = MockDataRepository.mateoGarcia.id
        val initialTotalUnread = repository.parentChats.value.sumOf { it.unreadCount }

        repository.markParentChatAsRead(childId)

        val updatedChat = repository.parentChats.value.first { it.child.id == childId }
        assertEquals(0, updatedChat.unreadCount)

        val newTotalUnread = repository.parentChats.value.sumOf { it.unreadCount }
        assertEquals(initialTotalUnread - 1, newTotalUnread)
    }

    @Test
    fun markWorkerChatAsRead_resetsWorkerChatUnreadCount() {
        val workerChatWithUnread = repository.workerChats.value.firstOrNull { it.unreadCount > 0 }
        assertTrue("Debe existir al menos un chat de trabajador con mensajes no leídos", workerChatWithUnread != null)

        val chatId = workerChatWithUnread!!.id
        repository.markWorkerChatAsRead(chatId)

        val updated = repository.workerChats.value.first { it.id == chatId }
        assertEquals(0, updated.unreadCount)
    }

    @Test
    fun markGlobalWorkerChatAsRead_resetsGlobalUnreadCount() {
        val globalChatId = repository.globalChat.value.id
        assertTrue("Global chat debe tener unread inicialmente", repository.globalChat.value.unreadCount > 0)

        repository.markWorkerChatAsRead(globalChatId)
        assertEquals(0, repository.globalChat.value.unreadCount)
    }

    @Test
    fun updateBitacoraChatMessage_updatesParentChatAndMovesToTop() {
        val childId = MockDataRepository.mateoGarcia.id
        val testText = "Hola, Mateo acaba de terminar su almuerzo."
        val testTime = "02:30 PM"

        // Caso 1: Mensaje saliente desde la educadora
        repository.updateBitacoraChatMessage(
            childId = childId,
            text = testText,
            time = testTime,
            isOutgoing = true
        )

        val firstChat = repository.parentChats.value.first()
        assertEquals(childId, firstChat.child.id)
        assertEquals("Yo: $testText", firstChat.lastMessage)
        assertEquals(testTime, firstChat.lastMessageTime)
        assertEquals(0, firstChat.unreadCount)

        // Caso 2: Mensaje entrante desde el familiar
        val incomingText = "Muchas gracias por avisar, nos vemos a la salida."
        val incomingTime = "02:45 PM"
        repository.updateBitacoraChatMessage(
            childId = childId,
            text = incomingText,
            time = incomingTime,
            isOutgoing = false
        )

        val updatedFirst = repository.parentChats.value.first()
        assertEquals(childId, updatedFirst.child.id)
        assertEquals(incomingText, updatedFirst.lastMessage)
        assertEquals(incomingTime, updatedFirst.lastMessageTime)
        assertEquals(1, updatedFirst.unreadCount)
    }

    @Test
    fun updateBitacoraChatMessage_updatesFamilyChatAndMovesToTop() {
        val mateoChildId = MockDataRepository.mateoGarcia.id
        val educator = MockDataRepository.getEducatorForChild(mateoChildId)
        val textOutgoing = "¿A qué hora merendará hoy?"
        val timeOutgoing = "03:10 PM"

        // Familiar envía mensaje saliente
        repository.updateBitacoraChatMessage(
            childId = mateoChildId,
            text = textOutgoing,
            time = timeOutgoing,
            isOutgoing = true
        )

        val familyChat = repository.familyChats.value.first()
        assertTrue(familyChat.title.equals(educator.name, ignoreCase = true))
        assertEquals("Yo: $textOutgoing", familyChat.lastMessage)
        assertEquals(timeOutgoing, familyChat.lastMessageTime)
        assertEquals(0, familyChat.unreadCount)

        // Educadora responde (mensaje entrante para familiar)
        val textIncoming = "A las 16:00 h tomamos la fruta."
        val timeIncoming = "03:15 PM"
        repository.updateBitacoraChatMessage(
            childId = mateoChildId,
            text = textIncoming,
            time = timeIncoming,
            isOutgoing = false
        )

        val updatedFamilyChat = repository.familyChats.value.first()
        assertTrue(updatedFamilyChat.title.equals(educator.name, ignoreCase = true))
        assertEquals("${educator.name}: $textIncoming", updatedFamilyChat.lastMessage)
        assertEquals(timeIncoming, updatedFamilyChat.lastMessageTime)
        assertEquals(1, updatedFamilyChat.unreadCount)
    }
}
