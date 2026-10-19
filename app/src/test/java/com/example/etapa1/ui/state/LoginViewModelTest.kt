package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val role = viewModel.submitLogin()
        assertNull(role)
        assertTrue(viewModel.uiState.value.hasError)
    }

    @Test
    fun submitLogin_withValidCredentials_succeeds() {
        viewModel.onEmailChanged("educadora@sonrisas.com")
        viewModel.onPasswordChanged("123456")
        viewModel.onRoleSelected(UserRole.EDUCADORA)

        val role = viewModel.submitLogin()
        assertEquals(UserRole.EDUCADORA, role)
        assertFalse(viewModel.uiState.value.hasError)
        assertTrue(viewModel.uiState.value.isSuccess)
    }
}
