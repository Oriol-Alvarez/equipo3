package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.model.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkRoomRepositoryImpl(
    private val api: SonrisasApi
) : RoomRepository {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    override val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    init {
        refresh()
    }

    /** Recarga las salas con sus contadores de asistencia de hoy. */
    override fun refresh() {
        RemoteSync.launch("Cargar salas") {
            val salas = api.getRooms().bodyOrLog("GET /rooms") ?: return@launch
            _rooms.value = salas.map { dto ->
                Room(
                    id = dto.id,
                    name = dto.name,
                    level = dto.level,
                    presentCount = dto.presentCount,
                    totalCount = dto.totalCount
                )
            }
        }
    }

    override fun limpiar() {
        _rooms.value = emptyList()
    }

    override fun getRoomById(roomId: String): Room? {
        return _rooms.value.find { it.id == roomId }
    }
}
