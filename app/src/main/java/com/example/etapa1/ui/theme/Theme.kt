package com.example.etapa1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlueHover,
    onSecondary = Color.White,
    background = AppBackground,
    surface = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderSubtle
)

@Composable
fun Etapa1Theme(
    content: @Composable () -> Unit
) {
    // Forzamos el esquema de colores a LightColorScheme para que siempre se vea igual
    // independientemente del modo oscuro o los colores dinámicos del sistema.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            // Forzamos el factor de escala de fuente a 1f para que el tamaño del texto
            // sea el mismo sin importar las configuraciones de accesibilidad del dispositivo.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 1f)
            ) {
                content()
            }
        }
    )
}