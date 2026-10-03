package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import kotlinx.coroutines.flow.StateFlow

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    val announcements: StateFlow<List<Announcement>> = announcementRepository.announcements
    val unreadCount: StateFlow<Int> = announcementRepository.unreadCount

    fun markAsRead(id: String) {
        announcementRepository.markAsRead(id)
    }
}
