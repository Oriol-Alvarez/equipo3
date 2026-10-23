package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SesionViewModelTest {

    private lateinit var authRepository: MockAuthRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        authRepository = MockAuthRepositoryImpl()
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun initialState_whenNotLoggedIn_startsInAnonimo() {
        val viewModel = SesionViewModel(authRepository, testScope)
        var latestState: SesionEstado? = null
        viewModel.estado.onEach { latestState = it }.launchIn(testScope)

        assertEquals(SesionEstado.Anonimo, latestState)
    }

    @Test
    fun initialState_whenAlreadyLoggedIn_startsInActivaWithSavedRole() {
        authRepository.setRole(UserRole.FAMILIAR)
        assertTrue(authRepository.isLoggedIn())

        val viewModel = SesionViewModel(authRepository, testScope)
        var latestState: SesionEstado? = null
        viewModel.estado.onEach { latestState = it }.launchIn(testScope)

        assertTrue("Debe iniciar en SesionEstado.Activa", latestState is SesionEstado.Activa)
        assertEquals(UserRole.FAMILIAR, (latestState as SesionEstado.Activa).sesion.role)
    }

    @Test
    fun iniciarSesion_transitionsToActiva() {
        val viewModel = SesionViewModel(authRepository, testScope)
        var latestState: SesionEstado? = null
        viewModel.estado.onEach { latestState = it }.launchIn(testScope)

        assertEquals(SesionEstado.Anonimo, latestState)

        viewModel.iniciarSesion(UserRole.ADMINISTRADOR)
        assertTrue("Debe pasar a SesionEstado.Activa", latestState is SesionEstado.Activa)
        assertEquals(UserRole.ADMINISTRADOR, (latestState as SesionEstado.Activa).sesion.role)
        assertTrue(authRepository.isLoggedIn())
    }

    @Test
    fun salir_transitionsToAnonimoAndLogsOut() {
        authRepository.setRole(UserRole.EDUCADORA)
        val viewModel = SesionViewModel(authRepository, testScope)
        var latestState: SesionEstado? = null
        viewModel.estado.onEach { latestState = it }.launchIn(testScope)

        assertTrue(latestState is SesionEstado.Activa)

        viewModel.salir()
        assertEquals(SesionEstado.Anonimo, latestState)
        assertEquals(false, authRepository.isLoggedIn())
    }
}
