package com.example.etapa1.data.repository

import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.StateFlow

interface BitacoraRepository {
    fun timelineForChild(childId: String): StateFlow<List<TimelineItem>>
    fun addChatMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean = true,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null
    )
    fun addEventChip(childId: String, time: String, text: String, iconType: String)
    fun addActivityCard(childId: String, time: String, category: String, portionLabel: String, description: String)
}
