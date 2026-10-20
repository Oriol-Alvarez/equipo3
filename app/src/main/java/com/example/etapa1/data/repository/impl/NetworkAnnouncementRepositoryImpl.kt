package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkAnnouncementRepositoryImpl(
    private val api: SonrisasApi,
    private val chatConexion: ChatConexion? = null
) : AnnouncementRepository {

    private val _announcements = MutableStateFlow<List<Announcement>>(MockDataRepository.getInitialAnnouncements())
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _unreadCount = MutableStateFlow(_announcements.value.count { it.isUnread })
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            fetchRemote()
        }
        if (chatConexion != null) {
            scope.launch {
                chatConexion.eventos.collect { evento ->
                    if (evento is EventoServidor.NuevoAviso ||
                        (evento is EventoServidor.Desconocido && evento.raw.contains("announcement"))
                    ) {
                        fetchRemote()
                    }
                }
            }
        }
    }

    override suspend fun refresh() {
        fetchRemote()
    }

    private suspend fun fetchRemote() = withContext(Dispatchers.IO) {
        try {
            val res = api.getAnnouncements()
            if (res.isSuccessful && res.body() != null) {
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

    override fun markAsRead(id: String) {
        _announcements.update { list ->
            list.map { if (it.id == id) it.copy(isUnread = false) else it }
        }
        _unreadCount.value = _announcements.value.count { it.isUnread }

        scope.launch {
            try {
                api.markAnnouncementAsRead(id)
            } catch (_: Exception) {
            }
        }
    }
}
