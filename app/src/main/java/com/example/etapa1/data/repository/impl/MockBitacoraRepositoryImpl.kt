package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockBitacoraRepositoryImpl : BitacoraRepository {
    private val _mateoTimeline = MutableStateFlow<List<TimelineItem>>(MockDataRepository.getInitialMateoTimeline())
    override val mateoTimeline: StateFlow<List<TimelineItem>> = _mateoTimeline.asStateFlow()

    override fun addChatMessage(
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) {
        val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val message = TimelineItem.ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            time = currentTime,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType
        )
        _mateoTimeline.update { it + message }
    }

    override fun addEventChip(time: String, text: String, iconType: String) {
        val chip = TimelineItem.EventChip(
            id = "chip_${System.currentTimeMillis()}",
            time = time,
            text = text,
            iconType = iconType
        )
        _mateoTimeline.update { it + chip }
    }

    override fun addActivityCard(time: String, category: String, portionLabel: String, description: String) {
        val card = TimelineItem.ActivityCard(
            id = "act_${System.currentTimeMillis()}",
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        _mateoTimeline.update { it + card }
    }
}
