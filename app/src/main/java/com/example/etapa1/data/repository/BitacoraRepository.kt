package com.example.etapa1.data.repository

import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface BitacoraRepository {
    val mateoTimeline: StateFlow<List<TimelineItem>>
    fun recargar(childId: String = "mateo_garcia")
    fun addChatMessage(
        text: String,
        isOutgoing: Boolean = true,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null,
        senderRole: UserRole? = null,
        date: String? = null
    )
    fun addEventChip(time: String, text: String, iconType: String, date: String? = null)
    fun addActivityCard(time: String, category: String, portionLabel: String, description: String, childId: String = "mateo_garcia", date: String? = null)
}
