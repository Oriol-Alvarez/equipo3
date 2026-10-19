package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class LoginViewModelTest {

    private lateinit var authRepository: MockAuthRepositoryImpl
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        authRepository = MockAuthRepositoryImpl()
        viewModel = LoginViewModel(authRepository)
    }

    @Test
    fun initialState_hasEmptyEmailAndEmptyPassword() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.hasError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun submitLogin_withEmptyCredentials_triggersError() {
        val role = runBlocking { viewModel.submitLogin() }
        assertNull(role)
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals("Por favor, introduce tu correo y contraseña.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitLogin_withValidCredentials_succeeds() {
        viewModel.onEmailChanged("educadora@sonrisas.com")
        viewModel.onPasswordChanged("123456")
        viewModel.onRoleSelected(UserRole.EDUCADORA)

        val role = runBlocking { viewModel.submitLogin() }
        assertEquals(UserRole.EDUCADORA, role)
        assertFalse(viewModel.uiState.value.hasError)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun submitLogin_whenRepositoryFails_setsSpecificErrorMessage() {
        val failingRepo = object : com.example.etapa1.data.repository.AuthRepository {
            override val currentUserRole = authRepository.currentUserRole
            override suspend fun login(email: String, password: String, role: UserRole): Result<UserRole> {
                return Result.failure(Exception("No se pudo conectar con el servidor. Verifica tu conexión."))
            }
            override fun setRole(role: UserRole) {}
            override fun logout() {}
        }
        val failingVm = LoginViewModel(failingRepo)
        failingVm.onEmailChanged("educadora@sonrisas.com")
        failingVm.onPasswordChanged("wrong")

        val role = runBlocking { failingVm.submitLogin() }
        assertNull(role)
        assertTrue(failingVm.uiState.value.hasError)
        assertEquals("No se pudo conectar con el servidor. Verifica tu conexión.", failingVm.uiState.value.errorMessage)

        // Typing again should clear the error
        failingVm.onPasswordChanged("new")
        assertFalse(failingVm.uiState.value.hasError)
        assertNull(failingVm.uiState.value.errorMessage)
    }
}
