package com.example.casosdeusopadre.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Reports : Screen("reports", "Reportes", Icons.Default.Assessment)
    object Photos : Screen("photos", "Fotos", Icons.Default.PhotoLibrary)
    object Certificate : Screen("certificate", "Certificado", Icons.Default.Description)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Reports,
    Screen.Photos,
    Screen.Certificate
)
