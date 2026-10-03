package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.theme.Etapa1Theme

@Composable
fun MoreMenuScreen(
    onNavigateToLostObjects: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Más",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Contenedor principal de opciones
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    
                    // Opción 2: Administración de Cuentas
                    MoreMenuItem(
                        icon = Icons.Default.ManageAccounts,
                        iconTint = BrandBlue,
                        iconBg = BrandBlueContainer,
                        title = "Administración de Cuentas",
                        onClick = { /* Informativo */ }
                    )

                    HorizontalDivider(
                        color = BorderSubtle,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Opción 3: Políticas de Privacidad
                    MoreMenuItem(
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF0D9488),
                        iconBg = Color(0xFFF0FDFA),
                        title = "Políticas de Privacidad",
                        onClick = { /* Informativo */ }
                    )

                    HorizontalDivider(
                        color = BorderSubtle,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Opción 4: Configuración de Notificaciones
                    MoreMenuItem(
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFF8B5CF6),
                        iconBg = Color(0xFFF5F3FF),
                        title = "Configuración de Notificaciones",
                        onClick = { /* Informativo */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botón Cerrar Sesión
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AlertRedBg,
                    contentColor = AlertRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRedBorder)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Cerrar Sesión",
                    tint = AlertRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar Sesión",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlertRed
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ir a $title",
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MoreMenuScreenPreview() {
    Etapa1Theme {
        MoreMenuScreen(
            onNavigateToLostObjects = {},
            onNavigateToHome = {},
            onLogout = {}
        )
    }
}

