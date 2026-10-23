package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAnnouncementRepositoryImpl
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: MockAnnouncementRepositoryImpl
    private lateinit var viewModel: AnnouncementsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockDataRepository.sharedAnnouncements.value = listOf(
            Announcement("ann_test_1", "Aviso 1", "Hoy", "Resumen 1", "Contenido 1", isUnread = true),
            Announcement("ann_test_2", "Aviso 2", "Hoy", "Resumen 2", "Contenido 2", isUnread = true),
            Announcement("ann_test_3", "Aviso 3", "Ayer", "Resumen 3", "Contenido 3", isUnread = false)
        )
        repository = MockAnnouncementRepositoryImpl()
        viewModel = AnnouncementsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasUnreadAnnouncements() {
        assertEquals(3, viewModel.announcements.value.size)
        assertEquals(2, viewModel.unreadCount.value)
        assertTrue(viewModel.announcements.value.any { it.isUnread })
    }

    @Test
    fun markAllAsRead_marksAllAnnouncementsAsReadAndResetsUnreadCount() {
        viewModel.markAllAsRead()

        assertFalse(viewModel.announcements.value.any { it.isUnread })
        assertEquals(0, viewModel.unreadCount.value)
        viewModel.announcements.value.forEach {
            assertFalse("Cada aviso debe estar marcado como leído", it.isUnread)
        }
    }

    @Test
    fun markAsRead_marksSpecificAnnouncementAsRead() {
        viewModel.markAsRead("ann_test_1")

        val ann1 = viewModel.announcements.value.first { it.id == "ann_test_1" }
        val ann2 = viewModel.announcements.value.first { it.id == "ann_test_2" }
        assertFalse(ann1.isUnread)
        assertTrue(ann2.isUnread)
        assertEquals(1, viewModel.unreadCount.value)
    }
}
