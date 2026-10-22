package com.example.etapa1.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Almacenamiento local persistente para trabajar sin conexión (RF-11).
 *
 * Guarda en el teléfono lo último que llegó del servidor (salas, niños, línea de tiempo) y la
 * cola de registros pendientes. Como son datos de menores, el archivo va cifrado con
 * EncryptedSharedPreferences (AES-256); si el teléfono no lo soporta se usa el archivo privado de la app.
 */
class OfflineStore(
    context: Context,
    nombre: String = "sonrisas_offline_cache",
    val json: Json = NetworkClient.json
) {
    @PublishedApi
    internal val prefs: SharedPreferences = abrir(context, nombre)

    inline fun <reified T> save(key: String, value: T) {
        try {
            val encoded = json.encodeToString(value)
            prefs.edit().putString(key, encoded).apply()
        } catch (_: Exception) {
            // Ignorar errores de serialización en caché
        }
    }

    inline fun <reified T> load(key: String): T? {
        val encoded = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(encoded)
        } catch (_: Exception) {
            null
        }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private fun abrir(context: Context, nombre: String): SharedPreferences = try {
            val llave = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context,
                "${nombre}_cifrado",
                llave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(nombre, Context.MODE_PRIVATE)
        }
    }
}
