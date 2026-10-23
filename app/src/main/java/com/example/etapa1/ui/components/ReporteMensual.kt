package com.example.etapa1.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.etapa1.data.remote.ReporteMensualDto
import com.example.etapa1.data.repository.ResultadoReporte
import com.example.etapa1.model.Child
import com.example.etapa1.ui.state.AppViewModelProvider
import com.example.etapa1.ui.state.ReporteMensualViewModel
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

private val NOMBRES_MES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** "2026-10" -> "Octubre" */
private fun nombreMes(mes: String): String =
    mes.substringAfter("-").toIntOrNull()?.let { NOMBRES_MES.getOrNull(it - 1) } ?: mes

/** Botón que abre el reporte mensual (RF-19). Va debajo del resumen semanal. */
@Composable
fun BotonReporteMensual(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ver reporte mensual", color = BrandBlue, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * RF-19: reporte del mes con los registros reales del niño. Si la estancia activó la IA, el texto
 * lo redacta la IA (el servidor no le manda el nombre del niño); si no, se arma con los mismos datos.
 * La ventana hereda el bloqueo de capturas de la app.
 */
@Composable
fun ReporteMensualDialog(
    child: Child,
    onDismiss: () -> Unit,
    vm: ReporteMensualViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by vm.estado.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(child.id) { vm.abrir(child.id) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reporte mensual", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(child.fullName, fontSize = 13.sp, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextPrimary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                estado.meses.forEach { mes ->
                    val elegido = mes == estado.mes
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (elegido) BrandBlue else CardBackground)
                            .border(1.dp, if (elegido) BrandBlue else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { vm.elegirMes(mes) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            nombreMes(mes),
                            fontSize = 13.sp,
                            fontWeight = if (elegido) FontWeight.Bold else FontWeight.Medium,
                            color = if (elegido) Color.White else TextPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (val r = estado.resultado) {
                    null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                    is ResultadoReporte.Error -> {
                        Text("No se pudo cargar el reporte: ${r.mensaje}", color = TextSecondary, fontSize = 14.sp)
                        Button(
                            onClick = { vm.reintentar() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) { Text("Reintentar") }
                    }
                    is ResultadoReporte.Listo -> ContenidoReporte(r.reporte, r.sinConexion) {
                        compartirReporteMensual(context, child, r.reporte)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ContenidoReporte(rep: ReporteMensualDto, sinConexion: Boolean, onCompartir: () -> Unit) {
    Text("${rep.monthLabel} · ${rep.periodText}", fontSize = 13.sp, color = TextSecondary)
    if (sinConexion) {
        Text(
            "Sin conexión: se muestra la última copia guardada en el teléfono.",
            fontSize = 12.sp,
            color = Color(0xFFB45309)
        )
    }

    Tarjeta {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BrandBlueContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                if (rep.generatedWithAi) "Redactado con IA a partir de los registros"
                else "Resumen automático con los registros del mes",
                fontSize = 11.sp,
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(rep.reportText, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
    }

    Tarjeta {
        Dato("Asistencia", "${rep.attendanceDays} de ${rep.schoolDays} días (${rep.attendancePercentageText})")
        Dato("Llegada promedio", rep.averageArrivalTime)
        Dato("Alimentación", rep.foodIntakePercentage)
        Dato("Siesta", if (rep.napDays > 0) "${rep.napDays} días · ${rep.averageNapDaily}" else "Sin registros")
        Dato("Ánimo", rep.moodSummary)
        Dato("Registros en la bitácora", rep.activitiesRecorded.toString())
    }

    Seccion("Para la familia", rep.pointsForFamily)
    Seccion("Comportamiento", rep.behaviorHighlights)
    Seccion(
        "Salud y seguridad",
        rep.healthEvents.ifEmpty { listOf("Sin accidentes ni situaciones de salud registradas.") }
    )
    Seccion("Actividades de la sala", rep.pedagogicalHighlights)
    Seccion("Observaciones semanales", rep.observations)

    OutlinedButton(onClick = onCompartir, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Default.Share, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Guardar o compartir reporte", color = BrandBlue, fontWeight = FontWeight.SemiBold)
    }
    if (rep.generatedAt.isNotBlank()) {
        Text("Generado el ${rep.generatedAt}", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenido() }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, fontSize = 13.sp, color = TextSecondary)
        Text(valor, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Seccion(titulo: String, renglones: List<String>) {
    if (renglones.isEmpty()) return
    Tarjeta {
        Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        renglones.forEach { Text("• $it", fontSize = 13.sp, color = TextPrimary) }
    }
}

fun compartirReporteMensual(context: Context, child: Child, rep: ReporteMensualDto) {
    val texto = buildString {
        appendLine("Reporte mensual de ${child.fullName} - Sonrisas de Cristal")
        appendLine("${rep.monthLabel} (${rep.periodText})")
        appendLine()
        appendLine(rep.reportText)
        appendLine()
        appendLine("Asistencia: ${rep.attendanceDays} de ${rep.schoolDays} días (${rep.attendancePercentageText})")
        appendLine("Llegada promedio: ${rep.averageArrivalTime}")
        appendLine("Alimentación: ${rep.foodIntakePercentage}")
        appendLine("Ánimo: ${rep.moodSummary}")
        if (rep.pointsForFamily.isNotEmpty()) {
            appendLine()
            appendLine("Para la familia:")
            rep.pointsForFamily.forEach { appendLine("- $it") }
        }
        if (rep.healthEvents.isNotEmpty()) {
            appendLine()
            appendLine("Salud y seguridad:")
            rep.healthEvents.forEach { appendLine("- $it") }
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Reporte mensual de ${child.fullName}")
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Guardar o compartir reporte"))
}
