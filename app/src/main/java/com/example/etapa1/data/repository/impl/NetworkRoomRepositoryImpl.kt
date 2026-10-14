package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkRoomRepositoryImpl(
    private val api: SonrisasApi
) : RoomRepository {

    private val _rooms = MutableStateFlow<List<Room>>(MockDataRepository.rooms)
    override val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getRooms()
                if (response.isSuccessful && response.body() != null) {
                    val remoteRooms = response.body()!!.map { dto ->
                        Room(
                            id = dto.id,
                            name = dto.name,
                            level = dto.level,
                            presentCount = dto.presentCount,
                            totalCount = dto.totalCount
                        )
                    }
                    if (remoteRooms.isNotEmpty()) {
                        _rooms.value = remoteRooms
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun getRoomById(roomId: String): Room? {
        return _rooms.value.find { it.id == roomId }
    }
}
