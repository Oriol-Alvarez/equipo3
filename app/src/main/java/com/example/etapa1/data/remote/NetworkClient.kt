package com.example.etapa1.data.remote

import com.example.etapa1.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    // Viene de "sonrisas.apiUrl" en local.properties (ver app/build.gradle.kts).
    val DEFAULT_BASE_URL: String = BuildConfig.API_BASE_URL
    var baseUrl: String = DEFAULT_BASE_URL

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
