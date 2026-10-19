package com.example.etapa1.data.repository

import com.example.etapa1.model.LostItem
import kotlinx.coroutines.flow.StateFlow

interface LostObjectsRepository {
    val lostItems: StateFlow<List<LostItem>>
    val claimedItems: StateFlow<Map<String, Boolean>>
    fun publishLostItem(newItem: LostItem)
    fun claimLostItem(itemId: String)
    fun deleteLostItem(itemId: String)

    /** Vuelve a pedir el catálogo al servidor. */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
