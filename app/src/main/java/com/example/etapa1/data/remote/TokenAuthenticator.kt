package com.example.etapa1.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrlProvider: () -> String
) : Authenticator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evitar bucles infinitos: si ya se reintentó una vez, desistir
        if (responseCount(response) >= 2) {
            tokenManager.notifySessionExpired()
            return null
        }

        // No intentar refrescar si la petición que falló ya era de auth
        val path = response.request.url.encodedPath
        if (path.contains("auth/refresh") || path.contains("auth/login")) {
            return null
        }

        synchronized(this) {
            val currentToken = tokenManager.token
            val requestAuth = response.request.header("Authorization")
            val requestToken = requestAuth?.removePrefix("Bearer ")?.trim()

            // Si otro hilo ya refrescó el token en paralelo, reintentar con el nuevo directamente
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.refreshToken
            if (refreshToken.isNullOrBlank()) {
                tokenManager.notifySessionExpired()
                return null
            }

            val base = baseUrlProvider()
            val normalizedBase = if (base.endsWith("/")) base else "$base/"
            val refreshUrl = "${normalizedBase}auth/refresh"

            val bodyJson = """{"refreshToken":"$refreshToken"}"""
            val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBody)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return try {
                val refreshResponse = client.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val responseStr = refreshResponse.body?.string()
                    if (!responseStr.isNullOrBlank()) {
                        val refreshDto = json.decodeFromString<RefreshResponseDto>(responseStr)
                        tokenManager.token = refreshDto.token
                        if (!refreshDto.refreshToken.isNullOrBlank()) {
                            tokenManager.refreshToken = refreshDto.refreshToken
                        }
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${refreshDto.token}")
                            .build()
                    } else {
                        tokenManager.notifySessionExpired()
                        null
                    }
                } else {
                    tokenManager.notifySessionExpired()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
