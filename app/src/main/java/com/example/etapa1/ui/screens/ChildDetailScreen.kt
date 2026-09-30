package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.ui.components.ChildFullDataContent
import com.example.etapa1.ui.components.ChildHeaderSummaryCard
import com.example.etapa1.ui.components.DailyBitacoraContent
import com.example.etapa1.ui.components.WeeklySummaryContent
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextSecondary

import com.example.etapa1.ui.state.ChildDetailViewModel

@Composable
fun ChildDetailScreen(
    viewModel: ChildDetailViewModel,
    child: Child,
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {}
) {
    ChildDetailScreen(
        child = child,
        onBack = onBack,
        onNavigateToChat = onNavigateToChat,
        onNavigateToAttendance = onNavigateToAttendance
    )
}

@Composable
fun ChildDetailScreen(
    child: Child,
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Datos del Niño (Default), 1 = Bitácora Diaria, 2 = Resumen Semanal
    val bitacoras = remember(child.id) { MockDataRepository.getDailyBitacorasForChild(child) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    val fullProfile = remember(child.id) { MockDataRepository.getChildFullProfile(child) }
    val weeklySummary = remember(child.id) { MockDataRepository.getWeeklySummaryForChild(child) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ficha del Alumno",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                    Text(
                        text = "${child.roomText} • ${child.groupText}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Abrir chat",
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Perfil Principal del Niño
            item {
                ChildHeaderSummaryCard(child = child)
            }

            // Selector de Pestañas: 1. Datos del Niño (Default) | 2. Bitácora Diaria | 3. Resumen Semanal
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CardBackground,
                    contentColor = BrandBlue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Datos del Niño",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Bitácora Diaria",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Resumen Semanal",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )
                }
            }

            // Contenido según la pestaña seleccionada
            when (selectedTab) {
                0 -> {
                    item {
                        ChildFullDataContent(
                            child = child,
                            fullProfile = fullProfile
                        )
                    }
                }
                1 -> {
                    item {
                        DailyBitacoraContent(
                            bitacoras = bitacoras,
                            selectedDayIndex = selectedDayIndex,
                            onSelectDay = { selectedDayIndex = it }
                        )
                    }
                }
                2 -> {
                    item {
                        WeeklySummaryContent(
                            summary = weeklySummary,
                            child = child
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChildDetailScreenPreview() {
    Etapa1Theme {
        ChildDetailScreen(
            child = MockDataRepository.mateoGarcia,
            onBack = {}
        )
    }
}
