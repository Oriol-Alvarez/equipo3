package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.model.Room
import kotlinx.coroutines.flow.StateFlow

class RoomSelectionViewModel(
    private val roomRepository: RoomRepository
) : ViewModel() {
    val rooms: StateFlow<List<Room>> = roomRepository.rooms

    /** Vuelve a pedir las salas y sus contadores de asistencia de hoy. */
    fun recargar() = roomRepository.refresh()
}
