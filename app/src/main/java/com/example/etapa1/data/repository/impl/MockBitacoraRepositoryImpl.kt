package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MockBitacoraRepositoryImpl : BitacoraRepository {
    // One stream per child; the repository keeps it alive during this demo session.
    private val timelines = mutableMapOf<String, MutableStateFlow<List<TimelineItem>>>()

    @Synchronized
    private fun timelineState(childId: String): MutableStateFlow<List<TimelineItem>> =
        timelines.getOrPut(childId) {
            val initialItems = if (childId == MockDataRepository.mateoGarcia.id) {
                MockDataRepository.getInitialMateoTimeline()
            } else {
                emptyList()
            }
            MutableStateFlow(initialItems)
        }

    override fun timelineForChild(childId: String): StateFlow<List<TimelineItem>> =
        timelineState(childId).asStateFlow()

    override fun addChatMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) {
        val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val message = TimelineItem.ChatMessage(
            id = "msg_${UUID.randomUUID()}",
            time = currentTime,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType
        )
        timelineState(childId).update { it + message }
    }

    override fun addEventChip(childId: String, time: String, text: String, iconType: String) {
        val chip = TimelineItem.EventChip(
            id = "chip_${UUID.randomUUID()}",
            time = time,
            text = text,
            iconType = iconType
        )
        timelineState(childId).update { it + chip }
    }

    override fun addActivityCard(childId: String, time: String, category: String, portionLabel: String, description: String) {
        val card = TimelineItem.ActivityCard(
            id = "act_${UUID.randomUUID()}",
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        timelineState(childId).update { it + card }
    }
}
