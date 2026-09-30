package com.example.etapa1.data.repository

import com.example.etapa1.model.LostItem
import kotlinx.coroutines.flow.StateFlow

interface LostObjectsRepository {
    val lostItems: StateFlow<List<LostItem>>
    val claimedItems: StateFlow<Map<String, Boolean>>
    fun publishLostItem(newItem: LostItem)
    fun claimLostItem(itemId: String)
}
