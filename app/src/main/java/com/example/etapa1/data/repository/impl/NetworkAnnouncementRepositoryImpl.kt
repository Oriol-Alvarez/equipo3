package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.model.Announcement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkAnnouncementRepositoryImpl(
    private val api: SonrisasApi,
    private val chatConexion: ChatConexion? = null
) : AnnouncementRepository {

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        refresh()
        // Cuando el servidor avisa que hay un aviso nuevo, se vuelve a pedir la lista.
        if (chatConexion != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                chatConexion.eventos.collect { evento ->
                    if (evento is EventoServidor.NuevoAviso ||
                        (evento is EventoServidor.Desconocido && evento.raw.contains("announcement"))
                    ) {
                        refresh()
                    }
                }
            }
        }
    }

    override fun refresh() {
        RemoteSync.launch("Cargar avisos") {
            val list = api.getAnnouncements().bodyOrLog("GET /announcements")?.map { dto ->
                Announcement(
                    id = dto.id,
                    title = dto.title,
                    date = dto.date,
                    summary = dto.summary,
                    fullContent = dto.fullContent,
                    isUnread = dto.isUnread,
                    author = dto.author
                )
            } ?: return@launch
            _announcements.value = list
            _unreadCount.value = list.count { it.isUnread }
        }
    }

    override fun limpiar() {
        _announcements.value = emptyList()
        _unreadCount.value = 0
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
