package com.example.etapa1.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1/"
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
            .addInterceptor(AuthInterceptor { tokenManager.token })
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
