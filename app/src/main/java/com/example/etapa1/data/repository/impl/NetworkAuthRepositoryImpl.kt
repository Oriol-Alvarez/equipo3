package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.DeviceTokenRequestDto
import com.example.etapa1.data.remote.LoginRequestDto
import com.example.etapa1.data.remote.RefreshRequestDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkAuthRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val initialRole = when (tokenManager.userRole?.uppercase()) {
        "FAMILIAR" -> UserRole.FAMILIAR
        "ADMINISTRADOR", "DIRECCION", "ADMIN" -> UserRole.ADMINISTRADOR
        else -> UserRole.EDUCADORA
    }

    private val _currentUserRole = MutableStateFlow(initialRole)
    override val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    override suspend fun login(email: String, password: String, role: UserRole): Result<UserRole> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequestDto(email = email.trim(), password = password, role = null))

            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!
                tokenManager.token = loginData.token
                tokenManager.refreshToken = loginData.refreshToken
                tokenManager.userId = loginData.user.id
                tokenManager.userRole = loginData.user.role
                tokenManager.userEmail = loginData.user.email
                tokenManager.userDisplayName = loginData.user.displayName

                val resolvedRole = when (loginData.user.role.uppercase()) {
                    "FAMILIAR" -> UserRole.FAMILIAR
                    "ADMINISTRADOR", "DIRECCION", "ADMIN" -> UserRole.ADMINISTRADOR
                    else -> UserRole.EDUCADORA
                }
                _currentUserRole.value = resolvedRole
                Result.success(resolvedRole)
            } else {
                val errorMsg = com.example.etapa1.data.remote.NetworkErrorHelper.parseResponse(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = com.example.etapa1.data.remote.NetworkErrorHelper.parseException(e)
            Result.failure(Exception(errorMsg, e))
        }
    }

    override fun setRole(role: UserRole) {
        _currentUserRole.value = role
        tokenManager.userRole = role.name
    }

    override fun logout() {
        val refresh = tokenManager.refreshToken
        val tokenSesion = tokenManager.token
        val telefono = tokenManager.deviceToken
        CoroutineScope(Dispatchers.IO).launch {
            // Este teléfono deja de recibir notificaciones de la cuenta que sale
            if (!tokenSesion.isNullOrBlank() && !telefono.isNullOrBlank()) {
                try {
                    api.unregisterDeviceToken("Bearer $tokenSesion", DeviceTokenRequestDto(deviceToken = telefono))
                } catch (_: Exception) {
                }
            }
            if (!refresh.isNullOrBlank()) {
                try {
                    api.logout(RefreshRequestDto(refresh))
                } catch (_: Exception) {
                }
            }
        }
        tokenManager.clear()
        _currentUserRole.value = UserRole.EDUCADORA
    }
}
