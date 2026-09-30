package com.example.casosdeusopadre.ui.screens.certificate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.casosdeusopadre.data.models.Child
import com.example.casosdeusopadre.ui.screens.home.StatusBar
import com.example.casosdeusopadre.ui.theme.AppShapes

// CU-04 Subir certificado médico (RF-19)
@Composable
fun CertificateScreen(viewModel: CertificateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.handleFileSelection(context, uri)
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Reusing StatusBar from Home for network toggle
            StatusBar(isOnline = uiState.isOnline, onToggle = { viewModel.toggleNetworkMode() })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Certificado Médico",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (uiState.children.isNotEmpty()) {
                    ChildSelectorSimple(
                        children = uiState.children,
                        selectedChild = uiState.selectedChild,
                        onChildSelected = { viewModel.selectChild(it) }
                    )
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Current Certificate Status
                    CertificateStatusCard(
                        isSynced = uiState.activeCertificate?.isSynced,
                        hasCertificate = uiState.activeCertificate != null
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Error Message
                    uiState.uploadError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Action Button
                    Button(
                        onClick = { 
                            // Only allow PDF, JPEG, PNG
                            filePickerLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = AppShapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.activeCertificate != null) "Reemplazar documento" else "Adjuntar documento",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateStatusCard(isSynced: Boolean?, hasCertificate: Boolean) {
    val bgColor = when {
        !hasCertificate -> MaterialTheme.colorScheme.surface
        isSynced == true -> Color(0xFFE8F5E9) // Light Green
        else -> Color(0xFFFFF3E0) // Light Orange
    }
    
    val contentColor = when {
        !hasCertificate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        isSynced == true -> Color(0xFF2E7D32) // Dark Green
        else -> Color(0xFFE65100) // Dark Orange
    }

    val icon = when {
        !hasCertificate -> Icons.Default.AttachFile
        isSynced == true -> Icons.Default.CheckCircle
        else -> Icons.Default.CloudOff
    }

    val statusText = when {
        !hasCertificate -> "Ningún documento adjunto."
        isSynced == true -> "Documento Activo y Sincronizado con Educadora"
        else -> "Pendiente de envío (Modo sin conexión)"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = AppShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = "Estado", tint = contentColor, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusText,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSelectorSimple(children: List<Child>, selectedChild: Child?, onChildSelected: (Child) -> Unit) {
    if (children.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            children.forEach { child ->
                FilterChip(
                    selected = child.id == selectedChild?.id,
                    onClick = { onChildSelected(child) },
                    label = { Text(child.name) }
                )
            }
        }
    } else {
        // Just show the single child
        selectedChild?.let { child ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = child.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
