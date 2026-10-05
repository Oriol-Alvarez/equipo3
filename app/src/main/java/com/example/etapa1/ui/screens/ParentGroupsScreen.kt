package com.example.etapa1.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.ui.components.WorkerChatConversationDialog
import com.example.etapa1.ui.components.WorkerChatItem
import com.example.etapa1.ui.state.FamilyGroupsViewModel
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Vista familiar: grupos a los que la educadora agregó a la familia.
 * La familia no crea grupos; solo lee y escribe en los que ya está.
 */
@Composable
fun ParentGroupsScreen(
    viewModel: FamilyGroupsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.useFamilyViewer()
    }

    ParentGroupsContent(
        groups = uiState.groups,
        isLoading = uiState.viewer == null,
        onBack = onBack,
        onSendMessage = viewModel::sendMessage
    )
}

@Composable
fun ParentGroupsContent(
    groups: List<WorkerChat>,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (
        groupId: String,
        text: String,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) -> Unit
) {
    var activeGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                        text = "Grupos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }

            when {
                isLoading -> Unit
                groups.isEmpty() -> EmptyGroupsMessage()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "groups_header") {
                        Text(
                            text = "TUS GRUPOS (${groups.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(groups, key = { it.id }) { group ->
                        WorkerChatItem(
                            chat = group,
                            onClick = { activeGroupId = group.id }
                        )
                    }
                }
            }
        }
    }

    val activeGroup = groups.find { it.id == activeGroupId }
    if (activeGroup != null) {
        WorkerChatConversationDialog(
            chat = activeGroup,
            onDismiss = { activeGroupId = null },
            onSendMessage = { text, uri, name, mime ->
                onSendMessage(activeGroup.id, text, uri?.toString(), name, mime)
            }
        )
    }
}

@Composable
private fun EmptyGroupsMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Todavía no estás en ningún grupo",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cuando la educadora te agregue a un grupo, aparecerá aquí.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ParentGroupsEmptyPreview() {
    Etapa1Theme {
        ParentGroupsContent(
            groups = emptyList(),
            onBack = {},
            onSendMessage = { _, _, _, _, _ -> }
        )
    }
}
