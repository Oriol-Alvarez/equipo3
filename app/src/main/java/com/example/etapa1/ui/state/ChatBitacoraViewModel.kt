package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.StateFlow

class ChatBitacoraViewModel(
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    val timelineItems: StateFlow<List<TimelineItem>> = bitacoraRepository.mateoTimeline

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            bitacoraRepository.addChatMessage(text.trim(), isOutgoing = true)
        }
    }
}
