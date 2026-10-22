package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.domain.Sesion
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn


sealed interface SesionEstado {
    data object Cargando : SesionEstado
    data object Anonimo : SesionEstado
    data class Activa(val sesion: Sesion) : SesionEstado
}

class SesionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /**
     * Si el teléfono ya tiene una sesión guardada, la app entra directo (aunque no haya internet,
     * RF-11) pero bloqueada: la persona tiene que poner su huella para usarla (RF-13).
     */
    private val sesionGuardada: UserRole? = authRepository.sesionGuardada()

    private val _isLoggedIn = MutableStateFlow(sesionGuardada != null)

    init {
        if (sesionGuardada != null) {
            authRepository.setRole(sesionGuardada)
            Bloqueo.bloquear()
        }
    }

    val estado: StateFlow<SesionEstado> = combine(
        authRepository.currentUserRole,
        _isLoggedIn
    ) { role, loggedIn ->
        if (loggedIn) {
            SesionEstado.Activa(Sesion(role = role))
        } else {
            SesionEstado.Anonimo
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = if (sesionGuardada != null) SesionEstado.Activa(Sesion(role = sesionGuardada)) else SesionEstado.Anonimo
    )

    /** Nombre de quien tiene la sesión, para la pantalla de bloqueo. */
    fun nombreSesion(): String? = authRepository.nombreSesion()

    fun iniciarSesion(role: UserRole) {
        authRepository.setRole(role)
        Bloqueo.desbloquear()
        _isLoggedIn.value = true
    }

    fun salir() {
        authRepository.logout()
        Bloqueo.desbloquear()
        _isLoggedIn.value = false
    }
}
