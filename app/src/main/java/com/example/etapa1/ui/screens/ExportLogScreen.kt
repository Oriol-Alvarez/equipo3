package com.example.etapa1.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.data.export.ExportedFile
import com.example.etapa1.model.Child
import com.example.etapa1.ui.state.ExportDayStatus
import com.example.etapa1.ui.state.ExportLogUiState
import com.example.etapa1.ui.state.ExportLogViewModel
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

/**
 * Exporta la "Bitácora de filtro diario" oficial de un niño en PDF, una semana a la vez.
 */
@Composable
fun ExportLogScreen(
    viewModel: ExportLogViewModel,
    child: Child,
    onBack: () -> Unit
) {
    LaunchedEffect(child.id) { viewModel.setChild(child) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    ExportLogContent(
        state = uiState,
        onBack = onBack,
        onSelectWeek = viewModel::selectWeek,
        onGenerate = viewModel::generate,
        onView = { file ->
            if (!openPdf(context, file)) viewModel.showError("No hay una app para abrir PDF. Usa Compartir.")
        },
        onShare = { file -> sharePdf(context, file, child) },
        onSaveToDownloads = viewModel::saveToDownloads
    )
}

private fun openPdf(context: Context, file: ExportedFile): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(file.uri), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun sharePdf(context: Context, file: ExportedFile, child: Child) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
        putExtra(Intent.EXTRA_SUBJECT, "Bitácora de filtro diario - ${child.fullName}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir bitácora"))
}

@Composable
fun ExportLogContent(
    state: ExportLogUiState,
    onBack: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onGenerate: () -> Unit,
    onView: (ExportedFile) -> Unit,
    onShare: (ExportedFile) -> Unit,
    onSaveToDownloads: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(color = CardBackground, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bitácora oficial (PDF)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        state.child?.let {
                            Text(it.fullName, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "intro") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandBlueContainer)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Genera la Bitácora de filtro diario con los registros de ingreso, estadía y egreso " +
                                "de la semana, en el formato oficial. Las firmas se recaban a mano al imprimir.",
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }

                item(key = "weeks_title") {
                    Text("Semana", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                item(key = "weeks") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.weeks) { index, week ->
                            val selected = index == state.selectedWeek
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) BrandBlue else CardBackground)
                                    .border(1.dp, if (selected) BrandBlue else BorderSubtle, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !state.isGenerating) { onSelectWeek(index) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    week.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else TextPrimary
                                )
                                Text(
                                    week.rangeText,
                                    fontSize = 10.5.sp,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                item(key = "days") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBackground)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Registros de la semana (${state.daysWithRecords} de 5 días)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        state.days.forEach { DayStatusRow(it) }
                    }
                }

                state.errorMessage?.let { message ->
                    item(key = "error") { Text(message, fontSize = 12.5.sp, color = AlertRed) }
                }

                item(key = "generate") {
                    Button(
                        onClick = onGenerate,
                        enabled = !state.isGenerating && state.daysWithRecords > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando…", color = Color.White)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.exported == null) "Generar PDF" else "Volver a generar", color = Color.White)
                        }
                    }
                }

                state.exported?.let { file ->
                    item(key = "result") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(1.dp, StatusPresentGreen, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                state.message ?: "PDF listo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusPresentGreen
                            )
                            Text("${file.pages} ${if (file.pages == 1) "página" else "páginas"} · tamaño carta horizontal", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onView(file) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver", color = BrandBlue)
                                }
                                OutlinedButton(onClick = { onShare(file) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compartir", color = BrandBlue)
                                }
                            }
                            if (state.canSaveToDownloads) {
                                OutlinedButton(
                                    onClick = onSaveToDownloads,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Guardar en Descargas", color = BrandBlue)
                                }
                            }
                        }
                    }
                }

                item(key = "bottom") { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DayStatusRow(day: ExportDayStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(day.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.width(64.dp))
        when {
            day.isFuture -> Text("Todavía no llega", fontSize = 12.sp, color = TextMuted)
            !day.hasAnyRecord -> Text("Sin registro (quedará en blanco)", fontSize = 12.sp, color = TextMuted)
            else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip("Ingreso", day.hasIngreso)
                StatusChip("Estadía", day.hasEstadia)
                StatusChip("Egreso", day.hasEgreso)
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, done: Boolean) {
    Text(
        text = if (done) "✓ $label" else "– $label",
        fontSize = 11.5.sp,
        color = if (done) StatusPresentGreen else TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (done) StatusPresentGreen.copy(alpha = 0.12f) else AppBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
