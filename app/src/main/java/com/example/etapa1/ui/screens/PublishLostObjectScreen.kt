package com.example.etapa1.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.LostItem
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishLostObjectScreen(
    roomName: String = "Sala 1A",
    onBack: () -> Unit,
    onPublishSuccess: (LostItem) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Ropa") }

    // Estados de validación de errores visuales (como en el mockup)
    var photoError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Abre la galería nativa de fotos de Android directamente
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            photoError = false
        }
    }

    val categories = listOf("Ropa", "Juguetes", "Útiles", "Otros")

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Top Bar - Centrado y consistente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = roomName,
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Títulos de la pantalla - Centrados y consistentes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Objetos Perdidos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Registra un objeto encontrado para publicarlo en el catálogo.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Recuadro de Fotografía (Abre Galería Nativa)
                val photoBorderColor = if (photoError) AlertRed else Color(0xFFCBD5E1)
                val photoShape = RoundedCornerShape(16.dp)

                val selectedBitmap = remember(selectedImageUri) {
                    if (!selectedImageUri.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(selectedImageUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(photoShape)
                        .background(if (photoError) AlertRedBg.copy(alpha = 0.3f) else Color(0xFFF8FAFC))
                        .border(
                            width = if (photoError) 1.8.dp else 1.2.dp,
                            color = photoBorderColor,
                            shape = photoShape
                        )
                        .clickable {
                            // Abrir directamente la galería nativa de imágenes
                            galleryLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap.asImageBitmap(),
                            contentDescription = "Foto seleccionada",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(photoShape),
                            contentScale = ContentScale.Crop
                        )
                        // Botón flotante para cambiar foto
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Cambiar imagen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (photoError) AlertRedBg else BrandBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Añadir fotografía",
                                    tint = if (photoError) AlertRed else BrandBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Añadir fotografía",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (photoError) AlertRed else BrandBlue
                            )
                        }
                    }
                }

                if (photoError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fotografía obligatoria",
                        fontSize = 12.sp,
                        color = AlertRed,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Campo: Descripción del Objeto
                Text(
                    text = "Descripción del Objeto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (it.isNotBlank()) descriptionError = false
                    },
                    placeholder = {
                        Text(
                            text = "Ej: Suéter azul marca X, talla 4",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    isError = descriptionError,
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = if (descriptionError) AlertRed else BorderSubtle,
                        errorBorderColor = AlertRed
                    )
                )

                if (descriptionError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Descripción obligatoria",
                        fontSize = 12.sp,
                        color = AlertRed,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Lugar de hallazgo (Opcional)
                Text(
                    text = "Lugar de hallazgo (Opcional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = {
                        Text(
                            text = "Ej: Patio de juegos",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Categoría
                Text(
                    text = "Categoría",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                categoryError = false
                            },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardBackground,
                                labelColor = TextSecondary,
                                selectedContainerColor = BrandBlueContainer,
                                selectedLabelColor = BrandBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BrandBlue else BorderSubtle,
                                selectedBorderColor = BrandBlue,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                if (categoryError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Debes seleccionar una categoría",
                        fontSize = 12.sp,
                        color = AlertRed,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Botón Publicar al Catálogo Central
                Button(
                    onClick = {
                        var hasError = false
                        // Validar campos obligatorios según diseño
                        if (selectedImageUri.isNullOrBlank()) {
                            photoError = true
                            hasError = true
                        }
                        if (description.isBlank()) {
                            descriptionError = true
                            hasError = true
                        }
                        if (selectedCategory.isBlank()) {
                            categoryError = true
                            hasError = true
                        }

                        if (!hasError) {
                            val title = if (description.contains(",")) {
                                description.substringBefore(",").trim()
                            } else {
                                description.take(25)
                            }

                            val newItem = LostItem(
                                id = "lost_${System.currentTimeMillis()}",
                                title = title.ifBlank { "Objeto Encontrado" },
                                description = description.trim(),
                                category = selectedCategory,
                                location = location.ifBlank { "Patio central" },
                                imageUri = selectedImageUri,
                                room = roomName
                            )
                            onPublishSuccess(newItem)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Publicar al Catálogo Central",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PublishLostObjectScreenPreview() {
    Etapa1Theme {
        PublishLostObjectScreen(
            roomName = "Sala 1A",
            onBack = {},
            onPublishSuccess = {}
        )
    }
}

