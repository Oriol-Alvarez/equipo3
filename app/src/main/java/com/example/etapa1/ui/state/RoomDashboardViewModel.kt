package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.StateFlow

class RoomDashboardViewModel(
    private val childRepository: ChildRepository
) : ViewModel() {
    /** Niños de la sala abierta con [abrirSala]. */
    val children: StateFlow<List<Child>> = childRepository.childrenOfRoom

    fun abrirSala(roomId: String) = childRepository.abrirSala(roomId)
}
