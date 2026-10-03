package com.example.etapa1.data.repository

import com.example.etapa1.model.Announcement
import kotlinx.coroutines.flow.StateFlow

interface AnnouncementRepository {
    val announcements: StateFlow<List<Announcement>>
    val unreadCount: StateFlow<Int>
    fun markAsRead(id: String)
}
