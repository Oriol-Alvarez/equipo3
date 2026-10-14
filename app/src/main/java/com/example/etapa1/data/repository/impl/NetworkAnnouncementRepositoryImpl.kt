package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkAnnouncementRepositoryImpl(
    private val api: SonrisasApi
) : AnnouncementRepository {

    private val _announcements = MutableStateFlow<List<Announcement>>(MockDataRepository.getInitialAnnouncements())
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _unreadCount = MutableStateFlow(_announcements.value.count { it.isUnread })
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.getAnnouncements()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val list = res.body()!!.map { dto ->
                        Announcement(
                            id = dto.id,
                            title = dto.title,
                            date = dto.date,
                            summary = dto.summary,
                            fullContent = dto.fullContent,
                            isUnread = dto.isUnread,
                            author = dto.author
                        )
                    }
                    _announcements.value = list
                    _unreadCount.value = list.count { it.isUnread }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun markAsRead(id: String) {
        _announcements.update { list ->
            list.map { if (it.id == id) it.copy(isUnread = false) else it }
        }
        _unreadCount.value = _announcements.value.count { it.isUnread }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.markAnnouncementAsRead(id)
            } catch (_: Exception) {
            }
        }
    }
}
