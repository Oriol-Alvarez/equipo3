package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    val announcements: StateFlow<List<Announcement>> = announcementRepository.announcements
    val unreadCount: StateFlow<Int> = announcementRepository.unreadCount

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            announcementRepository.refresh()
        }
    }

    fun markAsRead(id: String) {
        announcementRepository.markAsRead(id)
    }

    fun markAllAsRead() {
        announcementRepository.markAllAsRead()
    }
}
