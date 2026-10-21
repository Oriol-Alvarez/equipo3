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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
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
import com.example.etapa1.model.Child
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.components.ActiveChildSwitcher
import com.example.etapa1.ui.components.ImagePlaceholderBox
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
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

import androidx.compose.runtime.collectAsState
import com.example.etapa1.ui.state.LostObjectsViewModel
import com.example.etapa1.ui.state.LostObjectsUiState

@Composable
fun LostObjectsCatalogScreen(
    viewModel: LostObjectsViewModel,
    activeChild: Child? = null,
    onSwitchChild: (() -> Unit)? = null,
    onBack: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LostObjectsCatalogContent(
        uiState = uiState,
        activeChild = activeChild,
        onSwitchChild = onSwitchChild,
        onCategorySelect = viewModel::selectCategory,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleSearch = viewModel::toggleSearch,
        onClaimItem = { itemId ->
            viewModel.claimItem(itemId, activeChild?.fullName)
        },
        onDeleteItem = viewModel::deleteItem,
        onBack = onBack,
        onNavigateToPublish = onNavigateToPublish,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMessages = onNavigateToMessages
    )
}

@Composable
fun LostObjectsCatalogScreen(
    items: List<LostItem>,
    userRole: UserRole = UserRole.EDUCADORA,
    isWorkerRole: Boolean = (userRole != UserRole.FAMILIAR),
    activeChild: Child? = null,
    onSwitchChild: (() -> Unit)? = null,
    onBack: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onDeleteItem: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todos") }
    val claimedItems = remember { mutableStateMapOf<String, Boolean>() }
    val claimedByNames = remember { mutableStateMapOf<String, String>() }
    var currentItems by remember(items) { mutableStateOf(items) }

    val baseCategories = listOf("Todos", "Ropa", "Juguetes", "Útiles", "Otros")
    val categories = remember(currentItems) { (baseCategories + currentItems.map { it.category }).distinct() }

    val filtered = currentItems.filter { item ->
        val matchesCategory = selectedCategory == "Todos" || item.category.equals(selectedCategory, ignoreCase = true)
        val query = searchQuery.trim()
        val matchesSearch = query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true) ||
            (!item.location.isNullOrBlank() && item.location.contains(query, ignoreCase = true))
        matchesCategory && matchesSearch
    }

    val uiState = LostObjectsUiState(
        items = currentItems,
        categories = categories,
        selectedCategory = selectedCategory,
        searchQuery = searchQuery,
        isSearchOpen = isSearchOpen,
        isWorkerRole = isWorkerRole,
        claimedItemsMap = claimedItems,
        claimedByNamesMap = claimedByNames,
        filteredItems = filtered
    )

    LostObjectsCatalogContent(
        uiState = uiState,
        activeChild = activeChild,
        onSwitchChild = onSwitchChild,
        onCategorySelect = { selectedCategory = it },
        onSearchQueryChange = { searchQuery = it },
        onToggleSearch = {
            isSearchOpen = !isSearchOpen
            if (!isSearchOpen) searchQuery = ""
        },
        onClaimItem = { id ->
            claimedItems[id] = true
            val name = activeChild?.fullName ?: "Mateo García"
            claimedByNames[id] = name
        },
        onDeleteItem = { id ->
            currentItems = currentItems.filterNot { it.id == id }
            onDeleteItem(id)
        },
        onBack = onBack,
        onNavigateToPublish = onNavigateToPublish,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMessages = onNavigateToMessages
    )
}

@Composable
fun LostObjectsCatalogContent(
    uiState: LostObjectsUiState,
    activeChild: Child? = null,
    onSwitchChild: (() -> Unit)? = null,
    onCategorySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClaimItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit = {},
    onBack: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMessages: () -> Unit = {}
) {
    var itemToDelete by remember { mutableStateOf<LostItem?>(null) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeChild != null && onSwitchChild != null) {
                    Box(
                        modifier = Modifier.widthIn(min = 40.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar objeto perdido",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = "Objetos Perdidos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    Box(
                        modifier = Modifier.widthIn(min = 40.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        ActiveChildSwitcher(
                            child = activeChild,
                            onSwitchClick = onSwitchChild
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        text = "Objetos Perdidos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar objeto perdido",
                            tint = BrandBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Barra de búsqueda desplegable al pulsar la lupa
            AnimatedVisibility(visible = uiState.isSearchOpen) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
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
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
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
                items(uiState.categories) { category ->
                    val isSelected = uiState.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
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
            if (uiState.filteredItems.isEmpty()) {
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
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "No hay resultados para \"${uiState.searchQuery}\"."
                            } else {
                                "No hay objetos registrados en la categoría \"${uiState.selectedCategory}\"."
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
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        val isClaimed = uiState.claimedItemsMap[item.id] ?: item.isClaimed
                        val claimedByName = uiState.claimedByNamesMap[item.id] ?: item.claimedByName

                        LostItemCard(
                            item = item,
                            isClaimed = isClaimed,
                            claimedByName = claimedByName,
                            isWorkerRole = uiState.isWorkerRole,
                            onClaimClick = {
                                onClaimItem(item.id)
                            },
                            onDeleteClick = {
                                itemToDelete = item
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

    // Diálogo de confirmación para eliminar objeto cuando es reclamado o retirado
    if (itemToDelete != null) {
        val targetItem = itemToDelete!!
        val isTargetClaimed = uiState.claimedItemsMap[targetItem.id] ?: targetItem.isClaimed

        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar objeto perdido?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (isTargetClaimed) {
                        "¿Deseas retirar \"${targetItem.title}\" del catálogo?\n\nUsa esta opción una vez que el objeto haya sido reclamado y entregado en persona en la estancia."
                    } else {
                        "¿Deseas eliminar \"${targetItem.title}\" del catálogo?\n\nEsta opción permite retirar el objeto si fue reclamado y entregado en persona en la estancia."
                    },
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(targetItem.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Eliminar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null }
                ) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 13.5.sp
                    )
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun LostItemCard(
    item: LostItem,
    isClaimed: Boolean,
    claimedByName: String? = null,
    isWorkerRole: Boolean,
    onClaimClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
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

            // Título, Tipo de Objeto y Botón de eliminación en cabecera para el trabajador
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Sección de acciones inferiores según el rol
            if (!isWorkerRole) {
                // Rol Familiar: "Este objeto es de mi hijo" o "Reportado como mío"
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
                            text = if (!claimedByName.isNullOrBlank()) "Reclamado por $claimedByName" else "Reportado como mío",
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
            } else {
                // Rol Trabajador / Educadora
                Spacer(modifier = Modifier.height(12.dp))
                if (isClaimed) {
                    // Estado cuando una familia ya lo ha reclamado en la app
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
                                text = if (!claimedByName.isNullOrBlank()) "Reclamado por $claimedByName" else "Reclamado por familiar",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusPresentGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRedBg,
                            contentColor = AlertRed
                        ),
                        border = BorderStroke(1.dp, AlertRedBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AlertRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Eliminar (Entregado en estancia)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlertRed
                        )
                    }
                } else {
                    // Botón para eliminar cuando se reclama directamente en persona en la estancia
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRedBg,
                            contentColor = AlertRed
                        ),
                        border = BorderStroke(1.dp, AlertRedBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AlertRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Eliminar objeto (Reclamado en persona)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlertRed
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

