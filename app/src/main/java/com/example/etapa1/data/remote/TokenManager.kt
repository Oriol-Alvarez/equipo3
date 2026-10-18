package com.example.etapa1.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sonrisas_prefs", Context.MODE_PRIVATE)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    var token: String?
        get() = prefs.getString("jwt_token", null)
        set(value) = prefs.edit().putString("jwt_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()

    var userRole: String?
        get() = prefs.getString("user_role", null)
        set(value) = prefs.edit().putString("user_role", value).apply()

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) = prefs.edit().putString("user_email", value).apply()

    var userDisplayName: String?
        get() = prefs.getString("user_display_name", null)
        set(value) = prefs.edit().putString("user_display_name", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun notifySessionExpired() {
        clear()
        _sessionExpired.tryEmit(Unit)
    }
}
