package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenUserIsLoggedIn_startsInActiva() = runTest {
        val repo = MockAuthRepositoryImpl(initialIsLoggedIn = true)
        repo.setRole(UserRole.FAMILIAR)

        val viewModel = SesionViewModel(repo)

        val estado = viewModel.estado.value
        assertTrue(estado is SesionEstado.Activa)
        assertEquals(UserRole.FAMILIAR, (estado as SesionEstado.Activa).sesion.role)
    }

    @Test
    fun initialState_whenUserIsNotLoggedIn_startsInAnonimo() = runTest {
        val repo = MockAuthRepositoryImpl(initialIsLoggedIn = false)

        val viewModel = SesionViewModel(repo)

        val estado = viewModel.estado.value
        assertTrue(estado is SesionEstado.Anonimo)
    }

    @Test
    fun iniciarSesion_updatesStateToActiva() = runTest {
        val repo = MockAuthRepositoryImpl(initialIsLoggedIn = false)
        val viewModel = SesionViewModel(repo)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.estado.collect()
        }

        viewModel.iniciarSesion(UserRole.EDUCADORA)
        testScheduler.advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado is SesionEstado.Activa)
        assertEquals(UserRole.EDUCADORA, (estado as SesionEstado.Activa).sesion.role)

        job.cancel()
    }

    @Test
    fun salir_updatesStateToAnonimo() = runTest {
        val repo = MockAuthRepositoryImpl(initialIsLoggedIn = true)
        val viewModel = SesionViewModel(repo)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.estado.collect()
        }

        viewModel.salir()
        testScheduler.advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado is SesionEstado.Anonimo)

        job.cancel()
    }
}
