package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatBitacoraViewModel(
    private val bitacoraRepository: BitacoraRepository,
    private val chatConexion: ChatConexion? = null,
    private val messagesRepository: com.example.etapa1.data.repository.MessagesRepository? = null,
    /** Para mostrar los teléfonos reales del expediente en el botón de llamar. */
    private val childRepository: com.example.etapa1.data.repository.ChildRepository? = null
) : ViewModel() {

    val timelineItems: StateFlow<List<TimelineItem>> = bitacoraRepository.timeline

    val conexionEstado: StateFlow<EstadoConexion> =
        chatConexion?.estado ?: MutableStateFlow(EstadoConexion.Conectado)

    private var childId: String? = null

    /** Cambia cuando llegan datos del expediente; la pantalla lo usa para volver a leer [perfil]. */
    val cambiosFicha: StateFlow<Int> = childRepository?.cambios ?: MutableStateFlow(0)

    fun perfil(child: com.example.etapa1.model.Child): com.example.etapa1.model.ChildFullProfile? =
        childRepository?.getChildFullProfile(child)

    /** Abre la bitácora del niño y marca su chat como leído. */
    fun abrir(childId: String) {
        this.childId = childId
        bitacoraRepository.abrir(childId)
        messagesRepository?.markParentChatAsRead(childId)
        childRepository?.cargarDetalle(childId)
    }

    fun sendMessage(
        text: String,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null,
        senderRole: UserRole? = null,
        childId: String? = null
    ) {
        val nino = childId ?: this.childId ?: return
        if (text.isNotBlank() || fileUri != null) {
            // Sube el chat de la familia al inicio de la lista con el último mensaje.
            val hora = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val preview = text.trim().ifEmpty { if (fileUri != null) "📷 Archivo adjunto" else "" }
            messagesRepository?.updateBitacoraChatMessage(
                childId = nino,
                text = preview,
                time = hora,
                isOutgoing = true
            )
            bitacoraRepository.addChatMessage(
                childId = nino,
                text = text.trim(),
                isOutgoing = true,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType,
                senderRole = senderRole
            )
        }
    }

    fun deletePhoto(messageId: String, childId: String? = null) {
        val nino = childId ?: this.childId ?: return
        bitacoraRepository.deletePhoto(nino, messageId)
    }
}
