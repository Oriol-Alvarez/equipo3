package com.example.etapa1.data.repository

import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.flow.StateFlow

interface BitacoraRepository {
    val mateoTimeline: StateFlow<List<TimelineItem>>
    fun addChatMessage(text: String, isOutgoing: Boolean = true)
    fun addEventChip(time: String, text: String, iconType: String)
    fun addActivityCard(time: String, category: String, portionLabel: String, description: String)
}
