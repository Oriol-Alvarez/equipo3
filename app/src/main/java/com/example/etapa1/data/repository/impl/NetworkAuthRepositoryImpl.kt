package com.example.etapa1.data.repository.impl

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
import kotlinx.coroutines.runBlocking

class NetworkAuthRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val initialRole = when (tokenManager.userRole?.uppercase()) {
        "FAMILIAR" -> UserRole.FAMILIAR
        else -> UserRole.EDUCADORA
    }

    private val _currentUserRole = MutableStateFlow(initialRole)
    override val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    override fun login(email: String, password: String, role: UserRole): Result<UserRole> {
        return try {
            val response = runBlocking(Dispatchers.IO) {
                api.login(LoginRequestDto(email = email.trim(), password = password, role = role.name))
            }

            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!
                tokenManager.token = loginData.token
                tokenManager.refreshToken = loginData.refreshToken
                tokenManager.userRole = loginData.user.role
                tokenManager.userEmail = loginData.user.email
                tokenManager.userDisplayName = loginData.user.displayName

                val resolvedRole = when (loginData.user.role.uppercase()) {
                    "FAMILIAR" -> UserRole.FAMILIAR
                    else -> UserRole.EDUCADORA
                }
                _currentUserRole.value = resolvedRole
                Result.success(resolvedRole)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Código ${response.code()}"
                Result.failure(Exception("Error de autenticación: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setRole(role: UserRole) {
        _currentUserRole.value = role
        tokenManager.userRole = role.name
    }

    override fun logout() {
        val refresh = tokenManager.refreshToken
        if (!refresh.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
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
