package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.OfflineStore
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.RoomDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.model.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkRoomRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager? = null,
    private val cache: OfflineStore? = null
) : RoomRepository {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    override val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    init {
        refresh()
    }

    private fun clave() = "${tokenManager?.userId.orEmpty()}_salas"

    /** Recarga las salas con sus contadores de asistencia de hoy. */
    override fun refresh() {
        // RF-11: sin internet se muestran las salas guardadas en el teléfono
        if (_rooms.value.isEmpty()) {
            cache?.load<List<RoomDto>>(clave())?.let { _rooms.value = it.map(::aSala) }
        }
        RemoteSync.launch("Cargar salas") {
            val salas = api.getRooms().bodyOrLog("GET /rooms") ?: return@launch
            cache?.save(clave(), salas)
            _rooms.value = salas.map(::aSala)
        }
    }

    private fun aSala(dto: RoomDto) = Room(
        id = dto.id,
        name = dto.name,
        level = dto.level,
        presentCount = dto.presentCount,
        totalCount = dto.totalCount
    )

    override fun limpiar() {
        _rooms.value = emptyList()
    }

    override fun getRoomById(roomId: String): Room? {
        return _rooms.value.find { it.id == roomId }
    }
}
