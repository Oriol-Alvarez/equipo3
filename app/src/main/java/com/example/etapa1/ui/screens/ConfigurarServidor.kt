package com.example.etapa1.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.data.remote.NetworkClient
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Permite apuntar la app a otro servidor sin reinstalarla (por ejemplo, cuando el túnel de
 * Cloudflare cambia de dirección). Por la configuración de seguridad de red solo se aceptan
 * direcciones HTTPS (o el servidor local de desarrollo).
 */
@Composable
fun ConfigurarServidor() {
    val context = LocalContext.current
    var actual by remember { mutableStateOf(NetworkClient.baseUrl) }
    var abierto by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val host = actual.substringAfter("://").substringBefore("/")
    TextButton(onClick = {
        texto = actual.removeSuffix("/api/v1/")
        error = null
        abierto = true
    }) {
        Text("Servidor: $host", fontSize = 11.sp, color = TextSecondary)
    }

    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text("Servidor de Sonrisas") },
            text = {
                Column {
                    Text(
                        "Escribe la dirección del servidor, por ejemplo https://mi-tunel.trycloudflare.com",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it; error = null },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } }
                    )
                    TextButton(onClick = {
                        NetworkClient.restablecerServidor(context)
                        actual = NetworkClient.baseUrl
                        abierto = false
                    }) { Text("Usar el servidor original", fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nuevo = NetworkClient.cambiarServidor(context, texto)
                    if (nuevo == null) {
                        error = "Dirección no válida"
                    } else {
                        actual = nuevo
                        abierto = false
                    }
                }) { Text("Guardar", color = BrandBlue) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        )
    }
}
