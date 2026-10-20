package com.example.etapa1.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.etapa1.ui.screens.LoginScreen
import com.example.etapa1.ui.state.AppViewModelProvider
import com.example.etapa1.ui.state.LoginViewModel
import com.example.etapa1.ui.state.SesionEstado
import com.example.etapa1.ui.state.SesionViewModel

@Composable
fun SonrisasApp() {
    val sesionViewModel: SesionViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val estado by sesionViewModel.estado.collectAsState()

    when (val actual = estado) {
        SesionEstado.Cargando -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        SesionEstado.Anonimo -> {
            val loginViewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { role ->
                    sesionViewModel.iniciarSesion(role)
                }
            )
        }

        is SesionEstado.Activa -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as? com.example.etapa1.SonrisasApplication

            androidx.compose.runtime.LaunchedEffect(actual.sesion) {
                app?.container?.asegurarConexionChat()
                app?.container?.childRepository?.refresh()
                app?.container?.messagesRepository?.recargar()
                app?.container?.announcementRepository?.refresh()
            }

            LaunchedEffect(Unit) {
                app?.container?.tokenManager?.sessionExpired?.collect {
                    app.container.desconectarChat()
                    sesionViewModel.salir()
                }
            }

            SonrisasNavHost(
                sesion = actual.sesion,
                onSalir = {
                    app?.container?.desconectarChat()
                    sesionViewModel.salir()
                }
            )
        }
    }
}
