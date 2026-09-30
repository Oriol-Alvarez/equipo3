package com.example.etapa1.data.repository

import com.example.etapa1.model.Room
import kotlinx.coroutines.flow.StateFlow

interface RoomRepository {
    val rooms: StateFlow<List<Room>>
    fun getRoomById(roomId: String): Room?
}
