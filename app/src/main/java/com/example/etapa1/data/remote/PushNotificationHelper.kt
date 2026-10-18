package com.example.etapa1.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.etapa1.MainActivity
import com.example.etapa1.R

/**
 * Gestor de Notificaciones Push (FCM / Sistema) para mostrar alertas
 * cuando la app se encuentra en segundo plano o cerrada.
 */
object PushNotificationHelper {

    private const val CHANNEL_ID = "sonrisas_push_channel"
    private const val CHANNEL_NAME = "Notificaciones de Sonrisas"
    private const val CHANNEL_DESC = "Alertas de mensajes, urgencias y avisos de la estancia infantil"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            manager?.notify(notificationId, builder.build())
        }
    }

    fun registerDeviceToken(api: SonrisasApi, tokenManager: TokenManager, deviceToken: String) {
        tokenManager.deviceToken = deviceToken
        if (!tokenManager.token.isNullOrBlank()) {
            RemoteSync.launch("Registrar token FCM") {
                api.registerDeviceToken(DeviceTokenRequestDto(deviceToken = deviceToken))
            }
        }
    }
}
