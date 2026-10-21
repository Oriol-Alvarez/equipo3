package com.example.etapa1.data.remote

import android.util.Log
import com.example.etapa1.SonrisasApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SonrisasFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token recibido: $token")
        
        // Registrar el nuevo token en el backend
        val app = applicationContext as? SonrisasApplication
        if (app != null) {
            PushNotificationHelper.registerDeviceToken(
                api = app.container.api,
                tokenManager = app.container.tokenManager,
                deviceToken = token
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        // Si el mensaje contiene un payload de datos
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Payload de datos: ${remoteMessage.data}")
            val type = remoteMessage.data["type"]
            val title = remoteMessage.data["title"] ?: "Nueva notificación"
            val body = remoteMessage.data["body"] ?: "Tienes una actualización."

            // Podemos manejar lógica específica por tipo si es necesario
            when (type) {
                "new_announcement" -> {
                    PushNotificationHelper.showNotification(applicationContext, title, body)
                }
                "new_message" -> {
                    PushNotificationHelper.showNotification(applicationContext, title, body)
                }
                else -> {
                    PushNotificationHelper.showNotification(applicationContext, title, body)
                }
            }
        }

        // Si el mensaje contiene un payload de notificación (se muestra automáticamente si la app está en background)
        remoteMessage.notification?.let {
            Log.d("FCM", "Cuerpo de notificación: ${it.body}")
            // Mostramos manualmente la notificación si la app está en primer plano
            PushNotificationHelper.showNotification(
                context = applicationContext,
                title = it.title ?: "Sonrisas",
                body = it.body ?: "Tienes un mensaje nuevo"
            )
        }
    }
}
