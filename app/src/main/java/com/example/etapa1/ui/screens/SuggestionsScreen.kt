package com.example.etapa1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.theme.Etapa1Theme

/**
 * Pantalla de Buzón de Sugerencias donde el personal del centro puede consultar,
 * filtrar y responder las sugerencias publicadas por los padres de familia.
 */
@Composable
fun SuggestionsScreen(
    suggestions: List<ParentSuggestion>,
    onRespondSuggestion: (suggestionId: String, responseText: String, newStatus: SuggestionStatus) -> Unit = { _, _, _ -> },
    onNavigateToHome: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(SuggestionCategory.TODAS) }
    var selectedStatusFilter by remember { mutableStateOf<SuggestionStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var respondingSuggestion by remember { mutableStateOf<ParentSuggestion?>(null) }

    // Métricas rápidas
    val totalCount = suggestions.size
    val pendingCount = suggestions.count { it.status == SuggestionStatus.PENDIENTE }
    val inReviewCount = suggestions.count { it.status == SuggestionStatus.EN_REVISION }
    val resolvedCount = suggestions.count { it.status == SuggestionStatus.ATENDIDA }

    // Filtrado
    val filteredSuggestions = remember(suggestions, selectedCategory, selectedStatusFilter, searchQuery) {
        suggestions.filter { item ->
            val matchCategory = selectedCategory == SuggestionCategory.TODAS || item.category == selectedCategory
            val matchStatus = selectedStatusFilter == null || item.status == selectedStatusFilter
            val matchQuery = searchQuery.isBlank() ||
                    item.parentName.contains(searchQuery, ignoreCase = true) ||
                    item.childName.contains(searchQuery, ignoreCase = true) ||
                    item.subject.contains(searchQuery, ignoreCase = true) ||
                    item.content.contains(searchQuery, ignoreCase = true)
            matchCategory && matchStatus && matchQuery
        }
    }

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
                    text = "Buzón de Sugerencias",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Buscar sugerencia",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Barra de búsqueda desplegable
            AnimatedVisibility(visible = isSearchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por familia, alumno o tema...", fontSize = 13.sp, color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(48.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }

            // Tarjetas resumen de métricas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Total",
                    count = totalCount,
                    color = BrandBlue,
                    isSelected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Pendientes",
                    count = pendingCount,
                    color = Color(0xFFE65100),
                    isSelected = selectedStatusFilter == SuggestionStatus.PENDIENTE,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == SuggestionStatus.PENDIENTE) null else SuggestionStatus.PENDIENTE
                    },
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "En revisión",
                    count = inReviewCount,
                    color = Color(0xFF0288D1),
                    isSelected = selectedStatusFilter == SuggestionStatus.EN_REVISION,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == SuggestionStatus.EN_REVISION) null else SuggestionStatus.EN_REVISION
                    },
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Atendidas",
                    count = resolvedCount,
                    color = Color(0xFF2E7D32),
                    isSelected = selectedStatusFilter == SuggestionStatus.ATENDIDA,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == SuggestionStatus.ATENDIDA) null else SuggestionStatus.ATENDIDA
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Chips horizontales de categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandBlue else CardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BrandBlue else BorderSubtle,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Lista de sugerencias
            if (filteredSuggestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = BrandBlue.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay sugerencias para este filtro",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Las sugerencias que publiquen las familias aparecerán aquí.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "SUGERENCIAS DE FAMILIAS (${filteredSuggestions.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                        )
                    }

                    items(filteredSuggestions, key = { it.id }) { suggestion ->
                        SuggestionItemCard(
                            suggestion = suggestion,
                            onRespondClick = { respondingSuggestion = suggestion }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Modal para responder sugerencia
    respondingSuggestion?.let { suggestion ->
        RespondSuggestionDialog(
            suggestion = suggestion,
            onDismiss = { respondingSuggestion = null },
            onSubmitResponse = { responseText, newStatus ->
                onRespondSuggestion(suggestion.id, responseText, newStatus)
                respondingSuggestion = null
            }
        )
    }
}

/**
 * Chip de métrica rápida con filtro clicable
 */
@Composable
private fun MetricChip(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else CardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) color else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = if (isSelected) color else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tarjeta individual de sugerencia
 */
@Composable
private fun SuggestionItemCard(
    suggestion: ParentSuggestion,
    onRespondClick: () -> Unit
) {
    val initials = suggestion.parentName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "F" }

    val avatarColors = listOf(0xFF8E24AA, 0xFF0288D1, 0xFFD81B60, 0xFF00897B, 0xFFFB8C00, 0xFF5E35B1)
    val avatarBg = avatarColors[kotlin.math.abs(suggestion.parentName.hashCode()) % avatarColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Fila superior: Avatar, Nombre familia, Niño/Sala y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(avatarBg)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.parentName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${suggestion.childName} • ${suggestion.roomName}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "${suggestion.date}, ${suggestion.time}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Etiquetas: Categoría y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Categoría
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandBlueContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = suggestion.category.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue
                    )
                }

                // Estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(suggestion.status.bgColor))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (suggestion.status) {
                            SuggestionStatus.PENDIENTE -> {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(suggestion.status.color),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            SuggestionStatus.ATENDIDA -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(suggestion.status.color),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            else -> {}
                        }
                        Text(
                            text = suggestion.status.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(suggestion.status.color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Asunto
            Text(
                text = suggestion.subject,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Contenido / Mensaje
            Text(
                text = suggestion.content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Sección de Respuesta (si ya fue respondida)
            if (suggestion.response != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusPresentGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Respuesta del Centro",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusPresentGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = suggestion.response,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${suggestion.responderName ?: "Equipo Sonrisas"} • ${suggestion.responseDate ?: ""}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                // Botón para responder
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onRespondClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandBlue
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Responder",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo modal para que el educador responda formalmente a la sugerencia
 */
@Composable
private fun RespondSuggestionDialog(
    suggestion: ParentSuggestion,
    onDismiss: () -> Unit,
    onSubmitResponse: (responseText: String, newStatus: SuggestionStatus) -> Unit
) {
    var responseText by remember { mutableStateOf("") }
    var targetStatus by remember { mutableStateOf(SuggestionStatus.ATENDIDA) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBackground,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Responder Sugerencia",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Resumen de la sugerencia a la que se responde
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppBackground)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "${suggestion.parentName} (${suggestion.childName})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = suggestion.subject,
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Mensaje de respuesta institucional:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    placeholder = {
                        Text(
                            text = "Escribe una respuesta cordial informando a la familia...",
                            fontSize = 12.5.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Estado tras responder:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(SuggestionStatus.ATENDIDA, SuggestionStatus.EN_REVISION).forEach { status ->
                        val isSelected = targetStatus == status
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(status.bgColor) else AppBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(status.color) else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { targetStatus = status }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(status.color) else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón enviar
                Button(
                    onClick = {
                        if (responseText.isNotBlank()) {
                            onSubmitResponse(responseText.trim(), targetStatus)
                        }
                    },
                    enabled = responseText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(
                        text = "Enviar Respuesta a la Familia",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SuggestionsScreenPreview() {
    Etapa1Theme {
        SuggestionsScreen(
            suggestions = MockDataRepository.getInitialSuggestions(),
            onRespondSuggestion = { _, _, _ -> },
            onNavigateToHome = {}
        )
    }
}
