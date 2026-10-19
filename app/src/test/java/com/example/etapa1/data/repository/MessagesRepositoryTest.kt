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
}
