package com.example.etapa1.data.remote

import android.content.Context
import com.example.etapa1.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    const val DEFAULT_BASE_URL = "https://dormitory-landlady-detract.ngrok-free.dev/api/v1/"
    private const val PREFS_NAME = "sonrisas_config"
    private const val KEY_BASE_URL = "server_base_url"

    var baseUrl: String = DEFAULT_BASE_URL
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        if (!saved.isNullOrBlank()) {
            baseUrl = normalizeBaseUrl(saved)
        }
    }

    fun updateBaseUrl(context: Context, newUrl: String): String {
        val normalized = normalizeBaseUrl(newUrl)
        baseUrl = normalized
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
        return normalized
    }

    fun resetToDefault(context: Context): String {
        baseUrl = DEFAULT_BASE_URL
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BASE_URL)
            .apply()
        return DEFAULT_BASE_URL
    }

    fun normalizeBaseUrl(url: String): String {
        var trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        if (!trimmed.endsWith("/")) {
            trimmed = "$trimmed/"
        }
        if (!trimmed.contains("/api/v1/")) {
            trimmed = "${trimmed}api/v1/"
        }
        return trimmed
    }

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val currentBase = baseUrl.toHttpUrlOrNull()
                val newRequest = if (currentBase != null) {
                    val originalUrl = originalRequest.url
                    val newUrl = if (originalUrl.encodedPath.contains("/api/v1/")) {
                        val endpointPath = originalUrl.encodedPath.substringAfter("/api/v1/")
                        currentBase.resolve(endpointPath)?.newBuilder()
                            ?.encodedQuery(originalUrl.encodedQuery)
                            ?.build() ?: originalUrl.newBuilder()
                                .scheme(currentBase.scheme)
                                .host(currentBase.host)
                                .port(currentBase.port)
                                .build()
                    } else {
                        originalUrl.newBuilder()
                            .scheme(currentBase.scheme)
                            .host(currentBase.host)
                            .port(currentBase.port)
                            .build()
                    }
                    originalRequest.newBuilder()
                        .url(newUrl)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .build()
                } else {
                    originalRequest.newBuilder()
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .build()
                }
                chain.proceed(newRequest)
            }
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
