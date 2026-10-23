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
 * cuando la app está cerrada o en background (revisa una vez al día).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Iniciando chequeo de notificaciones en segundo plano...")
        val app = applicationContext as? SonrisasApplication ?: return Result.failure()

        try {
            // Revisamos si el usuario tiene una sesi├│n v├ílida guardada en el dispositivo
            val token = app.container.tokenManager.token
            if (token.isNullOrBlank()) {
                return Result.success() // No hay usuario, no hacemos nada
            }

            // 1. Revisar nuevos anuncios
            val announcementsResponse = app.container.api.getAnnouncements()
            if (announcementsResponse.isSuccessful) {
                val announcements = announcementsResponse.body()
                if (!announcements.isNullOrEmpty()) {
                    val latestAnnouncement = announcements.first()
                    val lastKnownId = app.container.tokenManager.lastAnnouncementId

                    if (latestAnnouncement.id != lastKnownId) {
                        PushNotificationHelper.showNotification(
                            context = applicationContext,
                            title = "Nuevo Anuncio: ${latestAnnouncement.title}",
                            body = latestAnnouncement.summary,
                            notificationId = 1001
                        )
                        app.container.tokenManager.lastAnnouncementId = latestAnnouncement.id
                    }
                }
            }

            // 2. Revisar nuevos mensajes (si es educadora/direcci├│n)
            if (app.container.tokenManager.userRole == "EDUCADORA" || app.container.tokenManager.userRole == "DIRECCION") {
                val chatsResponse = app.container.api.getWorkerChats()
                if (chatsResponse.isSuccessful) {
                    val totalUnread = chatsResponse.body()?.sumOf { it.unreadCount } ?: 0
                    val lastUnread = app.container.tokenManager.lastWorkerChatUnreadCount

                    if (totalUnread > lastUnread) {
                        PushNotificationHelper.showNotification(
                            context = applicationContext,
                            title = "Nuevos Mensajes",
                            body = "Tienes $totalUnread mensajes sin leer en tus chats.",
                            notificationId = 1002
                        )
                    }
                    // Actualizamos el contador en memoria
                    app.container.tokenManager.lastWorkerChatUnreadCount = totalUnread
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error al revisar notificaciones: ${e.message}")
            return Result.retry()
        }
    }

    companion object {
        fun setupPeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SonrisasSyncWorker",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }
    }
}
