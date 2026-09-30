package com.example.etapa1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.components.ImagePlaceholderBox
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.StatusPresentGreenBg
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun LostObjectsCatalogScreen(
    items: List<LostItem>,
    userRole: UserRole = UserRole.EDUCADORA,
    isWorkerRole: Boolean = (userRole != UserRole.FAMILIAR),
    onBack: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {}
) {
    // Estado local para registrar si un padre reclamó el objeto
    val claimedItems = remember { mutableStateMapOf<String, Boolean>() }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = remember(items) {
        val baseCategories = listOf("Todos", "Ropa", "Juguetes", "Útiles", "Otros")
        (baseCategories + items.map { it.category }).distinct()
    }

    val filteredItems = items.filter { item ->
        val matchesCategory = selectedCategory == "Todos" || item.category.equals(selectedCategory, ignoreCase = true)
        val query = searchQuery.trim()
        val matchesSearch = query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true) ||
            (!item.location.isNullOrBlank() && item.location.contains(query, ignoreCase = true))
        matchesCategory && matchesSearch
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
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Objetos Perdidos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) searchQuery = ""
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Buscar objeto perdido",
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Barra de búsqueda desplegable al pulsar la lupa
            AnimatedVisibility(visible = isSearchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Buscar por palabra clave...",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }

            // Botón Superior: + Reportar Objeto Perdido
            Button(
                onClick = onNavigateToPublish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Reportar Objeto Perdido",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Filtro por tipo de objetos (Chips de categoría)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
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

            Spacer(modifier = Modifier.height(4.dp))

            // Lista de Tarjetas del Catálogo o Estado Vacío
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron objetos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No hay resultados para \"$searchQuery\"."
                            } else {
                                "No hay objetos registrados en la categoría \"$selectedCategory\"."
                            },
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isClaimed = claimedItems[item.id] ?: item.isClaimed

                        LostItemCard(
                            item = item,
                            isClaimed = isClaimed,
                            isWorkerRole = isWorkerRole,
                            onClaimClick = {
                                claimedItems[item.id] = true
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LostItemCard(
    item: LostItem,
    isClaimed: Boolean,
    isWorkerRole: Boolean,
    onClaimClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            ImagePlaceholderBox(
                imageUri = item.imageUri,
                height = 175.dp,
                placeholderLabel = item.title
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Título y Tipo de Objeto a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandBlueContainer,
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = item.category,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Descripción
            Text(
                text = item.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (!item.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botón de acción: "Este objeto es de mi hijo" (solo para familias / no visible para rol trabajador)
            if (!isWorkerRole) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isClaimed) {
                    Button(
                        onClick = { /* Ya reclamado */ },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = StatusPresentGreenBg,
                            disabledContentColor = StatusPresentGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reportado como mío",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = onClaimClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Este objeto es de mi hijo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (isClaimed) {
                // Estado informativo para el trabajador cuando una familia ya lo ha reclamado
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(StatusPresentGreenBg)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusPresentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reclamado por familiar",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusPresentGreen
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LostObjectsCatalogScreenPreview() {
    Etapa1Theme {
        LostObjectsCatalogScreen(
            items = MockDataRepository.getInitialLostItems(),
            onBack = {},
            onNavigateToPublish = {},
            onNavigateToHome = {}
        )
    }
}

