package com.example.etapa1.data.repository

import com.example.etapa1.model.LostItem
import kotlinx.coroutines.flow.StateFlow

interface LostObjectsRepository {
    val lostItems: StateFlow<List<LostItem>>
    val claimedItems: StateFlow<Map<String, Boolean>>
    val claimedByNames: StateFlow<Map<String, String>>
    fun publishLostItem(newItem: LostItem)
    fun claimLostItem(itemId: String) { claimLostItem(itemId, null) }
    fun claimLostItem(itemId: String, childName: String?)
    fun deleteLostItem(itemId: String)

    /** Vuelve a pedir el catálogo al servidor. */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
