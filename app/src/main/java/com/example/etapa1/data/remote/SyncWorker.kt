package com.example.etapa1.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.example.etapa1.SonrisasApplication

/**
 * Worker en segundo plano (WorkManager) para revisar nuevos mensajes o anuncios
 * cuando la app está cerrada o en background (alternativa local a Firebase Cloud Messaging).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Iniciando chequeo de notificaciones en segundo plano...")
        val app = applicationContext as? SonrisasApplication ?: return Result.failure()

        try {
            // Revisamos si el usuario tiene una sesión válida guardada en el dispositivo
            val token = app.container.tokenManager.token
            if (token.isNullOrBlank()) {
                return Result.success() // No hay usuario, no hacemos nada
            }

            // Llamada rápida a la API para revisar mensajes o notificaciones no leídas
            val response = app.container.api.getStaffMembers() // Usado solo como Ping de prueba
            if (response.isSuccessful) {
                // Aquí consumiríamos un endpoint /notifications/unread real para saber qué alertar
                // Por propósitos de la demostración, dispararemos una notificación de aviso general si el túnel responde
                PushNotificationHelper.showNotification(
                    context = applicationContext,
                    title = "¡Nueva Notificación!",
                    body = "Tienes una actualización de tu Estancia Infantil."
                )
                return Result.success()
            }
            return Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error al revisar notificaciones: ${e.message}")
            return Result.retry()
        }
    }

    companion object {
        fun setupPeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SonrisasSyncWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
