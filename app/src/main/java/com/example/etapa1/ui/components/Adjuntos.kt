package com.example.etapa1.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.etapa1.SonrisasApplication
import com.example.etapa1.data.remote.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * Abre un archivo adjunto (foto, PDF) de un chat o de la bitácora.
 *
 * Los archivos del servidor solo se entregan con sesión (RF-09 / RF-14), así que no se pueden
 * abrir con el navegador: la app los descarga con el token a su carpeta privada y los abre con
 * el visor del teléfono mediante FileProvider. La carpeta se borra al cerrar sesión.
 */
object Adjuntos {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val CARPETA = "adjuntos"

    fun abrir(context: Context, uri: String, mime: String?, nombre: String? = null) {
        val app = context.applicationContext as? SonrisasApplication
        val esDelServidor = uri.startsWith("http") && uri.contains("/api/")
        if (!esDelServidor || app == null) {
            abrirLocal(context, Uri.parse(uri), mime)
            return
        }
        scope.launch {
            try {
                val cliente = NetworkClient.createOkHttpClient(app.container.tokenManager)
                cliente.newCall(Request.Builder().url(uri).build()).execute().use { respuesta ->
                    if (!respuesta.isSuccessful) {
                        val texto = if (respuesta.code == 404) "Este archivo ya no existe (pudo haberse eliminado)."
                        else "No se pudo abrir el archivo (HTTP ${respuesta.code})."
                        avisar(context, texto)
                        return@launch
                    }
                    val tipo = mime ?: respuesta.header("Content-Type")?.substringBefore(";")
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(tipo)
                        ?: nombre?.substringAfterLast('.', "")?.ifBlank { null } ?: "bin"
                    val carpeta = File(context.cacheDir, CARPETA).apply { mkdirs() }
                    val archivo = File(carpeta, "${uri.substringAfterLast('/').substringBefore('?')}.$extension")
                    respuesta.body?.byteStream()?.use { entrada ->
                        archivo.outputStream().use { salida -> entrada.copyTo(salida) }
                    }
                    val local = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                    withContext(Dispatchers.Main) { abrirLocal(context, local, tipo) }
                }
            } catch (e: Exception) {
                avisar(context, "Sin conexión: el archivo se podrá abrir cuando vuelva el internet.")
            }
        }
    }

    /** Borra las copias descargadas (al cerrar sesión, para que otra persona no las vea). */
    fun borrarDescargas(context: Context) {
        File(context.cacheDir, CARPETA).deleteRecursively()
    }

    private fun abrirLocal(context: Context, uri: Uri, mime: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay una app para abrir este tipo de archivo.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }

    private suspend fun avisar(context: Context, texto: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, texto, Toast.LENGTH_LONG).show()
    }
}
