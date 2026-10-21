package com.example.etapa1

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.data.remote.PushNotificationHelper
import com.example.etapa1.data.remote.SyncWorker
import com.example.etapa1.ui.navigation.SonrisasApp
import com.example.etapa1.ui.theme.Etapa1Theme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PushNotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // El teléfono se registra para notificaciones push al iniciar sesión (SonrisasApp).
        // Además, cada 15 min se revisan anuncios y mensajes aunque la app esté cerrada
        // (funciona aunque Firebase no esté configurado).
        SyncWorker.setupPeriodicSync(this)

        setContent {
            Etapa1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SonrisasApp()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    Etapa1Theme {
        SonrisasApp()
    }
}
