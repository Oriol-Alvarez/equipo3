package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.StateFlow

class ChatBitacoraViewModel(
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    fun timelineForChild(childId: String): StateFlow<List<TimelineItem>> =
        bitacoraRepository.timelineForChild(childId)

    fun sendMessage(
        childId: String,
        text: String,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null
    ) {
        if (text.isNotBlank() || fileUri != null) {
            bitacoraRepository.addChatMessage(
                childId = childId,
                text = text.trim(),
                isOutgoing = true,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType
            )
        }
    }
}
