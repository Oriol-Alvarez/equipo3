package com.example.etapa1.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

data class CenterPolicy(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val fullContent: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color
)

private val centerPoliciesList = listOf(
    CenterPolicy(
        id = "pol_privacy",
        title = "Política de Privacidad y Datos",
        category = "Protección de Datos",
        summary = "Uso de datos personales, imágenes y fotografías de los alumnos.",
        fullContent = "En nuestro centro educativo protegemos la privacidad de los alumnos y sus familias:\n\n" +
                "1. Tratamiento de Datos: Todos los datos personales recaudados en la matrícula y expediente digital se utilizan exclusivamente con fines educativos, médicos y de seguridad escolar.\n\n" +
                "2. Fotografía e Imágenes: La captura y publicación de imágenes en la aplicación del centro requiere la autorización previa firmada de los tutores legales. Las imágenes no se compartirán públicamente ni en redes sociales externas sin autorización explícita.\n\n" +
                "3. Confidencialidad: El personal del centro tiene prohibido divulgar información de los menores a terceros sin requerimiento legal.",
        icon = Icons.Default.Shield,
        iconTint = Color(0xFF0D9488),
        iconBg = Color(0xFFF0FDFA)
    ),
    CenterPolicy(
        id = "pol_attendance",
        title = "Asistencia, Horarios y Puntualidad",
        category = "Horarios y Asistencia",
        summary = "Horarios de entrada y salida, protocolo para faltas y retardos.",
        fullContent = "Para mantener la rutina pedagógica y tranquilidad de los niños:\n\n" +
                "1. Horario de Ingreso: El horario de entrada a las aulas es de 08:00 AM a 09:00 AM. Rogamos puntualidad para evitar interrupciones en la asamblea matutina.\n\n" +
                "2. Notificación de Ausencias: Cualquier falta o llegada tardía debe ser informada a través del chat o avisos de la aplicación antes de las 09:15 AM.\n\n" +
                "3. Salidas Anticipadas: Si el alumno requiere salir antes del horario habitual, el tutor debe avisar previamente indicando hora exacta y persona autorizada.",
        icon = Icons.Default.AccessTime,
        iconTint = Color(0xFF0288D1),
        iconBg = Color(0xFFE1F5FE)
    ),
    CenterPolicy(
        id = "pol_health",
        title = "Salud, Medicación y Alergias",
        category = "Salud Escolar",
        summary = "Protocolo de fiebre, administración de medicamentos y dietas.",
        fullContent = "Garantizar un entorno saludable es prioridad de la comunidad escolar:\n\n" +
                "1. Síntomas y Fiebre: Niños con fiebre superior a 37.5°C, vómitos, diarrea o síntomas contagiosos deben permanecer en casa. Si presentan síntomas en el centro, se contactará a los padres para su recogida en máximo 1 hora.\n\n" +
                "2. Medicación: Únicamente se administrarán medicamentos recetados por pediatra. Se debe adjuntar la receta médica oficial y la autorización firmada con dosis y horarios exactos.\n\n" +
                "3. Alergias e Intolerancias: Toda alergia debe contar con certificado médico actualizado para adaptar el menú del comedor escolar.",
        icon = Icons.Default.HealthAndSafety,
        iconTint = Color(0xFFD81B60),
        iconBg = Color(0xFFFCE4EC)
    ),
    CenterPolicy(
        id = "pol_security",
        title = "Seguridad y Recogida de Alumnos",
        category = "Seguridad",
        summary = "Personas autorizadas, identificación obligatoria y custodia.",
        fullContent = "Protocolos para la entrega segura de cada menor:\n\n" +
                "1. Personas Autorizadas: El menor solo será entregado a los tutores legales o a personas registradas en la ficha del alumno con copia de identificación oficial.\n\n" +
                "2. Personas No Registradas: Si una tercera persona no registrada acude a recoger al niño, se requiere autorización por escrito expresamente enviada por la app y validación telefónica por recepción.\n\n" +
                "3. Custodia Legal: Cualquier resolución judicial de custodia o restricción debe ser entregada formalmente en la dirección del centro.",
        icon = Icons.Default.VerifiedUser,
        iconTint = Color(0xFF7C3AED),
        iconBg = Color(0xFFF3E8FF)
    ),
    CenterPolicy(
        id = "pol_rules",
        title = "Reglamento Interno y Convivencia",
        category = "Normativa General",
        summary = "Identificación de ropa, objetos personales y comunicación.",
        fullContent = "Normas generales para la buena convivencia:\n\n" +
                "1. Marcado de Pertenencias: Toda la ropa de recambio, chaquetas, biberones y vasos deben estar etiquetados con el nombre completo del alumno.\n\n" +
                "2. Juguetes de Casa: Queda prohibido traer juguetes particulares de casa para prevenir pérdidas o discrepancias, salvo en actividades especiales notificadas.\n\n" +
                "3. Comunicación Respetuosa: Las consultas y observaciones entre familias y educadoras se encauzan mediante los canales oficiales de la aplicación.",
        icon = Icons.Default.Gavel,
        iconTint = Color(0xFFF59E0B),
        iconBg = Color(0xFFFEF3C7)
    )
)

@Composable
fun MoreMenuScreen(
    onNavigateToLostObjects: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedPolicy by remember { mutableStateOf<CenterPolicy?>(null) }

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
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sección 1: Configuración de Cuenta
                Text(
                    text = "CONFIGURACIÓN Y CUENTA",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MoreMenuItem(
                            icon = Icons.Default.ManageAccounts,
                            iconTint = BrandBlue,
                            iconBg = BrandBlueContainer,
                            title = "Administración de Cuentas",
                            subtitle = "Información del perfil y tutor",
                            onClick = { /* Informativo */ }
                        )

                        HorizontalDivider(
                            color = BorderSubtle,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        MoreMenuItem(
                            icon = Icons.Default.Notifications,
                            iconTint = Color(0xFF8B5CF6),
                            iconBg = Color(0xFFF5F3FF),
                            title = "Configuración de Notificaciones",
                            subtitle = "Avisos, alertas y recordatorios",
                            onClick = { /* Informativo */ }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sección 2: Sección de Políticas y Normativas del Centro
                Text(
                    text = "POLÍTICAS Y NORMATIVAS DEL CENTRO",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        centerPoliciesList.forEachIndexed { index, policy ->
                            MoreMenuItem(
                                icon = policy.icon,
                                iconTint = policy.iconTint,
                                iconBg = policy.iconBg,
                                title = policy.title,
                                subtitle = policy.summary,
                                onClick = { selectedPolicy = policy }
                            )

                            if (index < centerPoliciesList.lastIndex) {
                                HorizontalDivider(
                                    color = BorderSubtle,
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
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
                    border = BorderStroke(1.dp, AlertRedBorder)
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

    // Modal para mostrar detalles de la política seleccionada
    selectedPolicy?.let { policy ->
        PolicyDetailDialog(
            policy = policy,
            onDismiss = { selectedPolicy = null }
        )
    }
}

@Composable
private fun PolicyDetailDialog(
    policy: CenterPolicy,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera con Icono, Categoría y Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(policy.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = policy.icon,
                                contentDescription = null,
                                tint = policy.iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = policy.category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = policy.iconTint,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = policy.title,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Contenido desplazable de la política
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = policy.fullContent,
                        fontSize = 13.5.sp,
                        color = TextSecondary,
                        lineHeight = 21.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón Entendido / Cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Entendido",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
