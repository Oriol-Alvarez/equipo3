package com.example.etapa1.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.domain.EstadoConexion

/**
 * Indicador visual del estado de conexión del WebSocket (Práctica 12 - Charla).
 * Muestra el estado actual ("En línea", "Conectando…", "Reconectando en X s", "Sin conexión",
 * "Acceso rechazado") con un punto que pulsa suavemente mientras se intenta conectar o reconectar.
 */
@Composable
fun ChipConexion(
    estado: EstadoConexion,
    modifier: Modifier = Modifier
) {
    val (texto, puntoColor) = when (estado) {
        EstadoConexion.Conectado -> "En línea" to Color(0xFF2E7D32)
        EstadoConexion.Conectando -> "Conectando…" to Color(0xFFF57C00)
        is EstadoConexion.Reconectando -> "Reconectando en ${estado.enSegundos} s" to Color(0xFFF57C00)
        EstadoConexion.Desconectado -> "Sin conexión" to Color(0xFF757575)
        EstadoConexion.Rechazado -> "Acceso rechazado" to Color(0xFFD32F2F)
    }

    val late = estado == EstadoConexion.Conectando || estado is EstadoConexion.Reconectando
    val pulso by rememberInfiniteTransition(label = "pulso").animateFloat(
        initialValue = 1f,
        targetValue = if (late) 0.25f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulso"
    )

    Row(
        modifier = modifier
            .background(Color(0xFFE2E8F0).copy(alpha = 0.85f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .alpha(pulso)
                .background(puntoColor, CircleShape)
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color(0xFF1E293B)
        )
    }
}
