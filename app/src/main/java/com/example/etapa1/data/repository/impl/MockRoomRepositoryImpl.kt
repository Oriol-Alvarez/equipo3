package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockRoomRepositoryImpl : RoomRepository {
    private val _rooms = MutableStateFlow<List<Room>>(MockDataRepository.rooms)
    override val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    override fun getRoomById(roomId: String): Room? {
        return _rooms.value.find { it.id == roomId }
    }
}
