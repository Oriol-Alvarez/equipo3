package com.example.etapa1.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: (() -> Unit)? = null
) : Interceptor {
    constructor(tokenProvider: () -> String?) : this(tokenProvider, null)
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentToken = tokenProvider() ?: return chain.proceed(chain.request())
        val authenticatedRequest = chain.request().newBuilder()
            .header("Authorization", "Bearer $currentToken")
            .build()
        val response = chain.proceed(authenticatedRequest)
        if (response.code == 401) {
            onUnauthorized?.invoke()
        }
        return response
    }
}
