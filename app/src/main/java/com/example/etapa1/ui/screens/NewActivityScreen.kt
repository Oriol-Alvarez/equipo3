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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.PortionOption
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.components.MedicalAlertBanner
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
import com.example.etapa1.model.MockDataRepository

@Composable
fun NewActivityScreen(
    child: Child,
    onBack: () -> Unit,
    onSubmitActivity: (category: ActivityCategory, portion: PortionOption, comments: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ActivityCategory.ALIMENTACION) }
    var selectedPortion by remember { mutableStateOf(PortionOption.TODO) }
    var commentText by remember {
        mutableStateOf("Mateo disfrutó mucho su comida de hoy y pidió un poco más de fruta.")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Surface(
                color = CardBackground,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BrandBlue
                        )
                    }

                    Text(
                        text = "Nueva Actividad",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // balance center
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera con Foto y Datos de Mateo
                ChildAvatar(
                    initials = child.avatarInitials,
                    bgColor = Color(child.avatarBgColor),
                    size = 64.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = child.shortName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${child.ageText} • ${child.groupText}",
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Alerta médica si aplica
                if (child.allergyAlert != null) {
                    MedicalAlertBanner(
                        text = child.allergyAlert,
                        onClick = { /* Ver detalle certificado */ }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sección: Seleccionar Categoría
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Seleccionar Categoría",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid 2x2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryTile(
                            title = "Alimentación",
                            icon = Icons.Default.Restaurant,
                            isSelected = selectedCategory == ActivityCategory.ALIMENTACION,
                            onClick = { selectedCategory = ActivityCategory.ALIMENTACION },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryTile(
                            title = "Siesta",
                            icon = Icons.Default.NightlightRound,
                            isSelected = selectedCategory == ActivityCategory.SIESTA,
                            onClick = { selectedCategory = ActivityCategory.SIESTA },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryTile(
                            title = "Estado de Ánimo",
                            icon = Icons.Default.Face,
                            isSelected = selectedCategory == ActivityCategory.ESTADO_ANIMO,
                            onClick = { selectedCategory = ActivityCategory.ESTADO_ANIMO },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryTile(
                            title = "Otros",
                            icon = Icons.Default.MoreHoriz,
                            isSelected = selectedCategory == ActivityCategory.OTROS,
                            onClick = { selectedCategory = ActivityCategory.OTROS },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sección: Detalle de Alimentación
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detalle de Alimentación",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selector de Porción (Poco, Medio, Todo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PortionOption.values().forEach { portion ->
                                PortionTile(
                                    portion = portion,
                                    isSelected = selectedPortion == portion,
                                    onClick = { selectedPortion = portion },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Comentarios (Opcional)
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = {
                        Text(
                            text = "Comentarios (Opcional)",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Texto informativo
                Text(
                    text = "Esta actividad se registrará como un mensaje en la bitácora del chat.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Botón Enviar al Chat
                Button(
                    onClick = {
                        onSubmitActivity(selectedCategory, selectedPortion, commentText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar al Chat",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CategoryTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) BrandBlue else CardBackground)
            .border(
                1.dp,
                if (isSelected) BrandBlue else BorderSubtle,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else BrandBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PortionTile(
    portion: PortionOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) BrandBlue else BrandBlueContainer)
            .border(
                1.dp,
                if (isSelected) BrandBlue else Color(0xFFD4E3F7),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = portion.label,
                tint = if (isSelected) Color.White else BrandBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = portion.label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else BrandBlue
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewActivityScreenPreview() {
    Etapa1Theme {
        NewActivityScreen(
            child = MockDataRepository.mateoGarcia,
            onBack = {},
            onSubmitActivity = { _, _, _ -> }
        )
    }
}

