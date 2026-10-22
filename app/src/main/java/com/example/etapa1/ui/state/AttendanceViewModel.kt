package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.Horas
import com.example.etapa1.model.RegistroAsistencia
import kotlinx.coroutines.flow.StateFlow

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val childRepository: ChildRepository,
    /** Se conserva por compatibilidad: el registro diario lo actualiza el servidor con la asistencia. */
    @Suppress("unused") private val dailyLogRepository: DailyLogRepository? = null
) : ViewModel() {

    /** Niños de la sala abierta (la misma del tablero). */
    val children: StateFlow<List<Child>> = childRepository.childrenOfRoom

    /**
     * Guarda el ingreso o egreso con todos los datos del formulario (RF-03).
     *
     * La hora llega en 24 h ("14:30") y se muestra como "02:30 PM". Antes se le pegaba " AM"
     * al ingreso y " PM" al egreso, así que un ingreso a las 12:30 quedaba como medianoche.
     * El registro diario (hoja oficial) lo actualiza el servidor al recibir la asistencia; la app
     * ya no manda un segundo PUT que borraba los nombres de quien entrega y recibe.
     */
    fun saveAttendance(child: Child, registro: RegistroAsistencia) {
        val hora = Horas.a12h(registro.hora24)
        val statusText = if (registro.isIngreso) "$hora • Presente" else "$hora • Retirado"
        childRepository.updateChildAttendance(child.id, isPresent = registro.isIngreso, time = hora, statusText = statusText)
        attendanceRepository.registrarAsistencia(child, registro)
    }
}
