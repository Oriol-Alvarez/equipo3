package com.example.etapa1.data.remote

import android.content.Context
import android.net.Uri
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sube al servidor un archivo elegido en el teléfono (content://...) y devuelve la ruta
 * pública del servidor ("/api/v1/media/..."). Si ya es una ruta del servidor o una URL,
 * la devuelve igual. Devuelve null si no se pudo leer o subir.
 */
class SubidaArchivos(
    private val api: SonrisasApi,
    private val context: Context
) {
    suspend fun subir(uri: String, nombre: String?, mime: String?, carpeta: String): String? {
        if (uri.startsWith("http") || uri.startsWith("/")) return uri
        val nombreFinal = nombre ?: "archivo"
        val mimeFinal = mime ?: "application/octet-stream"
        val bytes = try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } ?: return null
        val parte = MultipartBody.Part.createFormData(
            "file", nombreFinal, bytes.toRequestBody(mimeFinal.toMediaTypeOrNull())
        )
        val folder = carpeta.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadMedia(parte, folder).bodyOrLog("POST /media/upload")?.url
    }
}
