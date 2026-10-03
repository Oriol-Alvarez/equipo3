package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockAnnouncementRepositoryImpl : AnnouncementRepository {

    private val _announcements = MutableStateFlow<List<Announcement>>(MockDataRepository.getInitialAnnouncements())
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _unreadCount = MutableStateFlow(_announcements.value.count { it.isUnread })
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    override fun markAsRead(id: String) {
        _announcements.update { list ->
            list.map { if (it.id == id) it.copy(isUnread = false) else it }
        }
        _unreadCount.value = _announcements.value.count { it.isUnread }
    }
}
