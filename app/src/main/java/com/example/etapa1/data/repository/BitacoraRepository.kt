package com.example.etapa1.data.repository

import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.StateFlow

/**
 * Bitácora (línea de tiempo y chat) de UN niño a la vez: el que está abierto en pantalla.
 *
 * [abrir] cambia de niño: limpia la línea de tiempo y la carga del servidor. Todo lo que se
 * agrega lleva el id del niño, así nunca se mezcla con la bitácora de otro.
 */
interface BitacoraRepository {
    /** Id del niño cuya bitácora está en [timeline] (null si aún no se abre ninguna). */
    val childIdActual: StateFlow<String?>
    val timeline: StateFlow<List<TimelineItem>>

    /** Abre (o recarga) la bitácora de ese niño. */
    fun abrir(childId: String)

    fun addChatMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean = true,
        fileUri: String? = null,
        fileName: String? = null,
        fileMimeType: String? = null,
        senderRole: UserRole? = null,
        date: String? = null
    )

    fun addEventChip(childId: String, time: String, text: String, iconType: String, date: String? = null)

    fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String? = null
    )

    fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String? = null,
        mealType: String? = null,
        consumptionOption: String? = null,
        napMinutes: Int? = null,
        moodState: String? = null,
        excretoryFunction: String? = null
    ) {
        addActivityCard(childId, time, category, portionLabel, description, date)
    }

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
