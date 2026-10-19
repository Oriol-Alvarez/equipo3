package com.example.etapa1.data.remote

import com.example.etapa1.SonrisasApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio de Firebase Cloud Messaging para la recepción de Notificaciones Push
 * en segundo plano o con la aplicación cerrada.
 */
class SonrisasFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? SonrisasApplication ?: return
        PushNotificationHelper.registerDeviceToken(
            api = app.container.api,
            tokenManager = app.container.tokenManager,
            deviceToken = token
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Aviso de Sonrisas"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "Tienes un nuevo mensaje o notificación."

        PushNotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body
        )
    }
}
