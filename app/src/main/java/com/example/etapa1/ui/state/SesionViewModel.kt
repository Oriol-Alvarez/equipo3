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

    private val _isLoggedIn = MutableStateFlow(false)

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
        initialValue = SesionEstado.Anonimo
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
