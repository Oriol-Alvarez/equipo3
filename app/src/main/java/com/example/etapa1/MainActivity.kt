package com.example.etapa1

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.example.etapa1.data.remote.PushNotificationHelper
import com.example.etapa1.data.remote.SyncWorker
import com.example.etapa1.ui.navigation.SonrisasApp
import com.example.etapa1.ui.state.Bloqueo
import com.example.etapa1.ui.theme.Etapa1Theme

/**
 * FragmentActivity (en lugar de ComponentActivity) porque la ventana de huella (BiometricPrompt, RF-13)
 * necesita una actividad con fragmentos.
 */
class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // RF-20 / RNF-07: bloquea capturas y grabaciones de pantalla en TODAS las pantallas
        // (los diálogos de Compose heredan la bandera) y oculta la vista previa en "recientes".
        // Solo una compilación de desarrollo hecha a propósito (-PpermitirCapturas=true) lo omite,
        // para tomar las imágenes del manual de usuario.
        if (!BuildConfig.PERMITIR_CAPTURAS) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

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

    override fun onStop() {
        super.onStop()
        Bloqueo.alSalir()
    }

    override fun onStart() {
        super.onStart()
        val haySesion = !(application as SonrisasApplication).container.tokenManager.token.isNullOrBlank()
        Bloqueo.alVolver(haySesion)
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    Etapa1Theme {
        SonrisasApp()
    }
}
