package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import com.example.etapa1.ui.state.ParentSuggestionsViewModel
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun ParentSuggestionsScreen(
    viewModel: ParentSuggestionsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ParentSuggestionsScreen(
        suggestions = uiState.mySuggestions,
        onSendSuggestion = { cat, subject, content ->
            viewModel.sendSuggestion(cat, subject, content)
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentSuggestionsScreen(
    suggestions: List<ParentSuggestion> = MockDataRepository.getInitialSuggestions(),
    onSendSuggestion: (SuggestionCategory, String, String) -> Boolean = { _, _, _ -> true },
    onBack: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(SuggestionCategory.INSTALACIONES) }
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        SuggestionCategory.INSTALACIONES,
        SuggestionCategory.ALIMENTACION,
        SuggestionCategory.ACTIVIDADES,
        SuggestionCategory.HORARIOS,
        SuggestionCategory.GENERAL
    )

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Top Bar
            item {
                Surface(
                    color = CardBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            text = "Buzón de Sugerencias",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Formulario para Enviar Nueva Sugerencia
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enviar Nueva Sugerencia",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tu opinión será revisada por el equipo directivo",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Categoría
                        Text(
                            text = "Categoría:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Text(
                                            text = cat.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = AppBackground,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Asunto
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {
                                subject = it
                                errorMessage = null
                            },
                            label = { Text("Asunto o título", fontSize = 13.sp) },
                            placeholder = { Text("Ej. Duda sobre menú vegano", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mensaje
                        OutlinedTextField(
                            value = content,
                            onValueChange = {
                                content = it
                                errorMessage = null
                            },
                            label = { Text("Detalle de tu sugerencia", fontSize = 13.sp) },
                            placeholder = { Text("Describe aquí tu propuesta o comentario para la escuela...", fontSize = 13.sp) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = AlertRed,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (showSuccessBanner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusPresentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "¡Sugerencia enviada correctamente!",
                                    fontSize = 12.sp,
                                    color = StatusPresentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botón Enviar
                        Button(
                            onClick = {
                                if (subject.isBlank() || content.isBlank()) {
                                    errorMessage = "Por favor completa el asunto y el mensaje."
                                } else {
                                    val success = onSendSuggestion(selectedCategory, subject, content)
                                    if (success) {
                                        subject = ""
                                        content = ""
                                        showSuccessBanner = true
                                        errorMessage = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
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
                                text = "Enviar Sugerencia a Dirección",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Historial de Sugerencias Enviadas
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MIS SUGERENCIAS ENVIADAS (${suggestions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            items(suggestions, key = { it.id }) { suggestion ->
                ParentSuggestionCard(suggestion = suggestion)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ParentSuggestionCard(suggestion: ParentSuggestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: Categoría + Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandBlueContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = suggestion.category.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Status chip
                val (statusBg, statusColor, statusLabel) = when (suggestion.status) {
                    SuggestionStatus.PENDIENTE -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Pendiente")
                    SuggestionStatus.EN_REVISION -> Triple(BrandBlueContainer, BrandBlue, "En revisión")
                    SuggestionStatus.ATENDIDA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Atendida")
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Asunto
            Text(
                text = suggestion.subject,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Fecha
            Text(
                text = suggestion.date,
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido
            Text(
                text = suggestion.content,
                fontSize = 13.5.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Respuesta oficial de la dirección si existe
            if (!suggestion.response.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F4F8))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${suggestion.responderName ?: "Dirección"} • ${suggestion.responseDate ?: "Hoy"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = suggestion.response,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ParentSuggestionsScreenPreview() {
    Etapa1Theme {
        ParentSuggestionsScreen()
    }
}
