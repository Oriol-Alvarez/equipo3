package com.example.etapa1.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Almacenamiento local persistente para soporte offline en repositorios de red.
 * Mantiene en disco los datos obtenidos del servidor para que estén disponibles
 * de forma inmediata incluso sin conexión a internet.
 */
class OfflineStore(context: Context, val json: Json = NetworkClient.json) {
    @PublishedApi
    internal val prefs: SharedPreferences =
        context.getSharedPreferences("sonrisas_offline_cache", Context.MODE_PRIVATE)

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
}
