package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatBitacoraViewModel(
    private val bitacoraRepository: BitacoraRepository,
    private val chatConexion: ChatConexion? = null
) : ViewModel() {

    val timelineItems: StateFlow<List<TimelineItem>> = bitacoraRepository.mateoTimeline

    val conexionEstado: StateFlow<EstadoConexion> =
        chatConexion?.estado ?: MutableStateFlow(EstadoConexion.Conectado)

    fun sendMessage(
        text: String,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null
    ) {
        if (text.isNotBlank() || fileUri != null) {
            bitacoraRepository.addChatMessage(
                text = text.trim(),
                isOutgoing = true,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType
            )
        }
    }
}
