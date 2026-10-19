package com.example.etapa1.data.remote

import android.util.Log
import com.example.etapa1.domain.SimpleDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

/**
 * Utilidades compartidas por los repositorios de red de grupos, observaciones, planeación y registro diario.
 *
 * Las interfaces de los repositorios son síncronas (las usan los ViewModels tal cual), así que los
 * repositorios de red guardan los datos en un StateFlow local, actualizan ese StateFlow al momento
 * y mandan el cambio al servidor en segundo plano.
 */
object RemoteSync {
    private const val TAG = "SonrisasRed"

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ejecuta [block] en segundo plano; si falla la red, traduce el error y lo registra en Logcat. */
    fun launch(what: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                val errorMsg = NetworkErrorHelper.parseException(e)
                Log.w(TAG, "$what: $errorMsg (Detalle técnico: ${e.message})")
            }
        }
    }

    /**
     * Cola que manda los cambios al servidor uno por uno y en el orden en que se hicieron
     * (por ejemplo: ingreso, luego estadía, luego egreso del mismo día).
     */
    class SerialQueue {
        private val tasks = Channel<Pair<String, suspend () -> Unit>>(Channel.UNLIMITED)

        init {
            scope.launch {
                for ((what, block) in tasks) {
                    try {
                        block()
                    } catch (e: Exception) {
                        val errorMsg = NetworkErrorHelper.parseException(e)
                        Log.w(TAG, "$what: $errorMsg (Detalle técnico: ${e.message})")
                    }
                }
            }
        }

        fun enqueue(what: String, block: suspend () -> Unit) {
            tasks.trySend(what to block)
        }
    }

    /** Cuerpo de la respuesta o null (y lo registra con mensaje en español) si el servidor contestó con error. */
    fun <T> Response<T>.bodyOrLog(what: String): T? {
        if (isSuccessful) return body()
        val friendly = NetworkErrorHelper.parseResponse(this)
        Log.w(TAG, "$what: $friendly (HTTP ${code()})")
        return null
    }

    /** Devuelve el mensaje amigable de error del servidor. */
    fun <T> Response<T>.errorMessage(): String = NetworkErrorHelper.parseResponse(this)

    /** Convierte una ruta del servidor ("/api/v1/media/x") en URL completa para mostrarla en la app. */
    fun absoluteUrl(uri: String): String {
        if (!uri.startsWith("/")) return uri
        val base = NetworkClient.baseUrl
        val origin = base.substringBefore("://") + "://" + base.substringAfter("://").substringBefore("/")
        return origin + uri
    }

    fun SimpleDate.toIso(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun parseIso(text: String): SimpleDate? {
        val parts = text.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return SimpleDate(y, m, d)
    }

    fun SimpleDate.plusDays(days: Int): SimpleDate {
        val c = toCalendar()
        c.add(Calendar.DAY_OF_MONTH, days)
        return SimpleDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }
}
