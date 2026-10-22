package com.example.etapa1.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.etapa1.ui.components.DaycareLogo
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.TextSecondary

/**
 * RF-13: la app abierta con una sesión guardada (o que estuvo un rato en segundo plano) queda
 * inutilizable hasta que la persona pone su huella o el PIN del teléfono.
 * Si el teléfono no tiene huella configurada, solo se puede entrar con la contraseña.
 */
@Composable
fun PantallaBloqueo(
    nombre: String?,
    onDesbloquear: () -> Unit,
    onUsarContrasena: () -> Unit
) {
    val context = LocalContext.current
    val actividad = remember(context) { context.buscarActividad() }
    val autenticadores = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK
    }
    val disponible = remember {
        BiometricManager.from(context).canAuthenticate(autenticadores) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var mensaje by remember { mutableStateOf<String?>(null) }

    fun pedirHuella() {
        val fa = actividad ?: return
        val prompt = BiometricPrompt(
            fa,
            ContextCompat.getMainExecutor(fa),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    mensaje = null
                    onDesbloquear()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) onUsarContrasena()
                    else mensaje = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    mensaje = "Huella no reconocida. Intenta otra vez."
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sonrisas de Cristal")
            .setSubtitle(if (nombre.isNullOrBlank()) "Desbloquea para continuar" else "Sesión de $nombre")
            .setAllowedAuthenticators(autenticadores)
            .apply { if (autenticadores == BIOMETRIC_WEAK) setNegativeButtonText("Usar contraseña") }
            .build()
        prompt.authenticate(info)
    }

    // Mientras está bloqueada, "atrás" manda la app al fondo en lugar de mostrar lo de abajo
    BackHandler { (context as? Activity ?: actividad)?.moveTaskToBack(true) }

    LaunchedEffect(Unit) {
        if (disponible) pedirHuella()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            // Ningún toque llega a las pantallas de abajo
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DaycareLogo(size = 76.dp)
            Spacer(Modifier.height(16.dp))
            Text("App bloqueada", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (disponible) {
                    "Pon tu huella para continuar" + (if (nombre.isNullOrBlank()) "." else " como $nombre.")
                } else {
                    "Este teléfono no tiene huella ni PIN configurado. Entra con tu contraseña."
                },
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            mensaje?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(28.dp))
            if (disponible) {
                Button(
                    onClick = { pedirHuella() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Desbloquear con huella", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onUsarContrasena) {
                Text(if (disponible) "Entrar con otra cuenta o contraseña" else "Entrar con contraseña", color = BrandBlue)
            }
        }
    }
}

private fun Context.buscarActividad(): FragmentActivity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is FragmentActivity) return actual
        actual = actual.baseContext
    }
    return null
}
