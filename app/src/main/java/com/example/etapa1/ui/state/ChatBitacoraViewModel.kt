package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatBitacoraViewModel(
    private val bitacoraRepository: BitacoraRepository,
    private val chatConexion: ChatConexion? = null,
    coroutineScope: CoroutineScope? = null,
    private val messagesRepository: MessagesRepository? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _currentChildId = MutableStateFlow("mateo_garcia")
    val currentChildId: StateFlow<String> = _currentChildId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val timelineItems: StateFlow<List<TimelineItem>> = _currentChildId
        .flatMapLatest { childId -> bitacoraRepository.getTimelineForChild(childId) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = bitacoraRepository.mateoTimeline.value
        )

    val conexionEstado: StateFlow<EstadoConexion> =
        chatConexion?.estado ?: MutableStateFlow(EstadoConexion.Conectado)

    fun recargar(childId: String = "mateo_garcia") {
        _currentChildId.value = childId
        bitacoraRepository.recargar(childId)
    }

    fun sendMessage(
        text: String,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null,
        senderRole: UserRole? = null,
        childId: String? = null
    ) {
        val targetChild = childId ?: _currentChildId.value
        if (text.isNotBlank() || fileUri != null) {
            val hora = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val preview = text.trim().ifEmpty { if (fileUri != null) "📷 Archivo adjunto" else "" }
            messagesRepository?.updateBitacoraChatMessage(
                childId = targetChild,
                text = preview,
                time = hora,
                isOutgoing = true
            )
            bitacoraRepository.addChatMessageForChild(
                childId = targetChild,
                text = text.trim(),
                isOutgoing = true,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType,
                senderRole = senderRole
            )
        }
    }
}
