package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.domain.Sesion
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.CoroutineScope
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

class SesionViewModel(
    private val authRepository: AuthRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())

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
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = if (authRepository.isLoggedIn()) {
            SesionEstado.Activa(Sesion(role = authRepository.currentUserRole.value))
        } else {
            SesionEstado.Anonimo
        }
    )

    fun iniciarSesion(role: UserRole) {
        authRepository.setRole(role)
        _isLoggedIn.value = true
    }

    fun salir() {
        authRepository.logout()
        _isLoggedIn.value = false
    }
}
