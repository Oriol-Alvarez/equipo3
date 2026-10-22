package com.example.etapa1.data.remote

import android.content.Context
import com.example.etapa1.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    /** Servidor con el que se compiló la app (se puede cambiar con -PapiUrl=... al compilar). */
    val DEFAULT_BASE_URL: String = BuildConfig.API_URL

    /**
     * Servidor que usa la app ahora. La dirección puede cambiar sin reinstalar (por ejemplo, cuando
     * se levanta otro túnel de Cloudflare): se guarda en el teléfono desde la pantalla de inicio de sesión.
     */
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    private const val PREFS = "sonrisas_servidor"
    private const val CLAVE = "api_url"

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    /** Lee el servidor guardado (si lo hay). Se llama al abrir la app, antes de cualquier petición. */
    fun cargarServidorGuardado(context: Context) {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null)
        if (!guardado.isNullOrBlank()) baseUrl = normalizar(guardado)
    }

    /**
     * Cambia el servidor. Acepta "https://algo.trycloudflare.com" y le agrega "/api/v1/".
     * Devuelve null si la dirección no es válida.
     */
    fun cambiarServidor(context: Context, direccion: String): String? {
        val url = normalizar(direccion)
        val parseada = url.toHttpUrlOrNull() ?: return null
        if (parseada.host.isBlank()) return null
        baseUrl = url
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE, url).apply()
        return url
    }

    fun restablecerServidor(context: Context) {
        baseUrl = DEFAULT_BASE_URL
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(CLAVE).apply()
    }

    private fun normalizar(direccion: String): String {
        var url = direccion.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        url = url.trimEnd('/')
        if (!url.endsWith("/api/v1")) url = url.substringBefore("/api") + "/api/v1"
        return "$url/"
    }

    /** Manda cada petición de la API al servidor elegido en ese momento. */
    private class ServidorActual : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val peticion = chain.request()
            val destino = baseUrl.toHttpUrlOrNull() ?: return chain.proceed(peticion)
            val actual = peticion.url
            if (!actual.encodedPath.startsWith("/api/")) return chain.proceed(peticion)
            if (actual.host == destino.host && actual.port == destino.port && actual.scheme == destino.scheme) {
                return chain.proceed(peticion)
            }
            val nueva = actual.newBuilder().scheme(destino.scheme).host(destino.host).port(destino.port).build()
            return chain.proceed(peticion.newBuilder().url(nueva).build())
        }
    }

    fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        // Seguridad: nunca se escribe en Logcat el cuerpo ni las cabeceras (token, datos de menores).
        // En la versión de desarrollo solo se registra método, ruta y código de respuesta.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(ServidorActual())
            .addInterceptor(AuthInterceptor(
                tokenProvider = { tokenManager.token },
                onUnauthorized = { tokenManager.notifySessionExpired() }
            ))
            .addInterceptor(logging)
            .pingInterval(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun createRetrofit(tokenManager: TokenManager, url: String = baseUrl): Retrofit {
        val normalizedUrl = if (url.trim().endsWith("/")) url.trim() else "${url.trim()}/"
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(createOkHttpClient(tokenManager))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createApi(tokenManager: TokenManager, url: String = baseUrl): SonrisasApi {
        return createRetrofit(tokenManager, url).create(SonrisasApi::class.java)
    }
}
