package com.example.etapa1.data.repository

import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.StateFlow

interface ChildRepository {
    /** Sala abierta por la educadora (id del servidor, p. ej. "sala_2b"). */
    val salaActual: StateFlow<String?>

    /** Niños de [salaActual]. */
    val childrenOfRoom: StateFlow<List<Child>>

    /** Hijas e hijos de la familia con sesión iniciada. */
    val parentChildren: StateFlow<List<Child>>

    /**
     * Aumenta cada vez que llegan del servidor expedientes, resúmenes o historiales.
     * Las pantallas lo observan para volver a leer con [getChildFullProfile] y compañía.
     */
    val cambios: StateFlow<Int>

    /** Abre una sala y carga sus niños. */
    fun abrirSala(roomId: String)

    fun getChildById(childId: String): Child?
    fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String)

    /** Pide al servidor el expediente, los resúmenes y el historial de ese niño. */
    fun cargarDetalle(childId: String)

    fun getChildFullProfile(child: Child): ChildFullProfile
    fun updateChildFullProfile(childId: String, profile: ChildFullProfile)
    fun getWeeklySummaryForChild(child: Child): WeeklySummary
    fun getWeeklySummariesForChild(child: Child): List<WeeklySummary>
    fun markWeeklySummaryReadStatus(childId: String, weekRangeText: String, isRead: Boolean, timestampText: String? = null)
    fun setWeeklySummaryReaction(childId: String, weekRangeText: String, reaction: String?)
    fun getDailyBitacorasForChild(child: Child): List<DailyBitacora>

    /** Vuelve a pedir al servidor las listas de niños (al iniciar sesión o al tocar recargar). */
    fun refresh()

    /** Borra lo cargado (al cerrar sesión), para que el siguiente usuario no vea datos ajenos. */
    fun limpiar() {}
}
