package com.example.casosdeusopadre

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.casosdeusopadre.ui.navigation.AppNavigation
import com.example.casosdeusopadre.ui.theme.CasosDeUsoPadreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // RF-17: Bloquear capturas de pantalla (Aplicado a toda la actividad)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        setContent {
            CasosDeUsoPadreTheme {
                AppNavigation()
            }
        }
    }
}
