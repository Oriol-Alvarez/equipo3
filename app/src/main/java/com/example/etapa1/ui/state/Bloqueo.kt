package com.example.etapa1.ui.state

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RF-13 "Desbloqueo con huella": si la sala usa una tableta compartida, la app queda bloqueada
 * hasta que la persona con sesión pone su huella (o el PIN del teléfono).
 *
 * Se bloquea al abrir la app con una sesión guardada y al regresar después de [ESPERA_MS] en segundo plano.
 */
object Bloqueo {
    /** Tiempo en segundo plano tras el cual se pide otra vez la huella. */
    const val ESPERA_MS = 60_000L

    private val _bloqueada = MutableStateFlow(false)
    val bloqueada: StateFlow<Boolean> = _bloqueada.asStateFlow()

    private var enSegundoPlanoDesde: Long? = null

    fun bloquear() {
        _bloqueada.value = true
    }

    fun desbloquear() {
        _bloqueada.value = false
    }

    /** La app pasó a segundo plano. */
    fun alSalir() {
        enSegundoPlanoDesde = SystemClock.elapsedRealtime()
    }

    /** La app regresó al frente: si pasó mucho tiempo y hay sesión, se bloquea. */
    fun alVolver(haySesion: Boolean) {
        val desde = enSegundoPlanoDesde ?: return
        enSegundoPlanoDesde = null
        if (haySesion && SystemClock.elapsedRealtime() - desde >= ESPERA_MS) bloquear()
    }
}
