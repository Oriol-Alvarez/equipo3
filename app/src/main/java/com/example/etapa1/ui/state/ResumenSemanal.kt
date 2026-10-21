package com.example.etapa1.ui.state

import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.WeeklySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Una semana escolar (lunes a viernes). */
data class SemanaEscolar(val lunes: Date, val viernes: Date, val etiqueta: String, val rango: String)

/** Resumen calculado solo con los registros reales de una semana. */
data class ResumenSemanal(
    val semana: SemanaEscolar,
    val diasAsistencia: Int,
    val entradaMasTemprana: String?,
    val comidasRegistradas: Int,
    val comidasCompletas: Int,
    val minutosSiesta: Int,
    val diasConSiesta: Int,
    val animoFrecuente: String?,
    val registrosAnimo: Int,
    val totalRegistros: Int,
    val texto: String
) {
    val asistenciaTexto: String get() = "$diasAsistencia/5 días"
    val asistenciaDetalle: String
        get() = entradaMasTemprana?.let { "Entrada más temprana $it" } ?: "Sin ingresos registrados"

    val alimentacionTexto: String
        get() = if (comidasRegistradas == 0) "Sin datos" else "${comidasCompletas * 100 / comidasRegistradas}%"
    val alimentacionDetalle: String
        get() = if (comidasRegistradas == 0) "Sin comidas registradas"
        else "$comidasCompletas de $comidasRegistradas comidas completas"

    val descansoTexto: String
        get() = if (diasConSiesta == 0) "Sin datos" else CalculadoraResumenSemanal.formatoDuracion(minutosSiesta)
    val descansoDetalle: String
        get() = if (diasConSiesta == 0) "Sin siestas registradas"
        else "Prom. ${CalculadoraResumenSemanal.formatoDuracion(minutosSiesta / diasConSiesta)}/día"

    val animoTexto: String get() = animoFrecuente ?: "Sin datos"
    val animoDetalle: String
        get() = if (registrosAnimo == 0) "Sin registros de ánimo" else "$registrosAnimo registro(s) en la semana"
}

object CalculadoraResumenSemanal {

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    /** La semana actual y las [cuantas] - 1 anteriores, de la más reciente a la más antigua. */
    fun semanas(hoy: Date = Date(), cuantas: Int = 4): List<SemanaEscolar> {
        val base = Calendar.getInstance().apply {
            time = hoy
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            // Retrocede hasta el lunes (sábado y domingo cuentan para la semana que termina)
            val dia = get(Calendar.DAY_OF_WEEK)
            val atras = if (dia == Calendar.SUNDAY) 6 else dia - Calendar.MONDAY
            add(Calendar.DAY_OF_MONTH, -atras)
        }
        return (0 until cuantas).map { i ->
            val lunes = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7 * i) }
            val viernes = (lunes.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 4) }
            val dL = lunes.get(Calendar.DAY_OF_MONTH)
            val dV = viernes.get(Calendar.DAY_OF_MONTH)
            val mL = MESES[lunes.get(Calendar.MONTH)]
            val mV = MESES[viernes.get(Calendar.MONTH)]
            val corto = if (mL == mV) "$dL - $dV ${mV.take(3)}" else "$dL ${mL.take(3)} - $dV ${mV.take(3)}"
            val rango = if (mL == mV) "Semana del $dL al $dV de $mV, ${viernes.get(Calendar.YEAR)}"
            else "Semana del $dL de $mL al $dV de $mV, ${viernes.get(Calendar.YEAR)}"
            SemanaEscolar(
                lunes = lunes.time,
                viernes = viernes.time,
                etiqueta = if (i == 0) "Semana actual" else corto,
                rango = rango
            )
        }
    }

    fun calcular(
        nombre: String,
        childId: String,
        semana: SemanaEscolar,
        timeline: List<TimelineItem>,
        asistencias: List<AttendanceRecord>
    ): ResumenSemanal {
        val items = timeline.filter { enSemana(parseFecha(it.date), semana) }

        // Asistencia: ingresos formales del servidor + entradas marcadas en la bitácora
        val ingresos = asistencias.filter {
            it.childId == childId && it.isIngreso && enSemana(parseFecha(it.date), semana)
        }
        val diasIngreso = ingresos.mapNotNull { parseFecha(it.date)?.let(::claveDia) }.toSet() +
            items.filter { it is TimelineItem.EventChip && it.iconType == "entry" }
                .mapNotNull { parseFecha(it.date)?.let(::claveDia) }
        val entrada = ingresos.minByOrNull { minutosDelDia(it.time) ?: Int.MAX_VALUE }?.time

        val actividades = items.filterIsInstance<TimelineItem.ActivityCard>()

        val comidas = actividades.filter { it.category.equals("Alimentación", ignoreCase = true) }
        val completas = comidas.count {
            val p = it.portionLabel.lowercase(Locale.ROOT)
            p.contains("todo") || p.contains("pidió más") || p.contains("pidio mas")
        }

        val siestas = actividades.filter {
            it.category.equals("Descanso", true) || it.category.equals("Siesta", true)
        }
        var minutos = 0
        val diasSiesta = mutableSetOf<String>()
        siestas.forEach { s ->
            val m = Regex("(\\d+)\\s*min").find(s.portionLabel)?.groupValues?.get(1)?.toIntOrNull()
            if (m != null && m > 0) {
                minutos += m
                parseFecha(s.date)?.let { diasSiesta += claveDia(it) }
            }
        }

        val animos = actividades
            .filter { it.category.equals("Estado de Ánimo", true) }
            .mapNotNull { limpiarAnimo(it.portionLabel) } +
            items.filter { it is TimelineItem.EventChip && it.iconType == "mood" }
                .mapNotNull { limpiarAnimo((it as TimelineItem.EventChip).text) }
        val animoFrecuente = animos.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        val base = ResumenSemanal(
            semana = semana,
            diasAsistencia = diasIngreso.size.coerceAtMost(5),
            entradaMasTemprana = entrada,
            comidasRegistradas = comidas.size,
            comidasCompletas = completas,
            minutosSiesta = minutos,
            diasConSiesta = diasSiesta.size,
            animoFrecuente = animoFrecuente,
            registrosAnimo = animos.size,
            totalRegistros = items.size + ingresos.size,
            texto = ""
        )
        return base.copy(texto = redactar(nombre, base))
    }

    /** Redacta el resumen a partir de los números; no inventa nada que no esté registrado. */
    internal fun redactar(nombre: String, r: ResumenSemanal): String {
        if (r.totalRegistros == 0) {
            return "Todavía no hay registros de $nombre en esta semana. " +
                "El resumen se llena con la asistencia y las actividades que registre el personal."
        }
        val partes = mutableListOf<String>()
        partes += when {
            r.diasAsistencia == 0 -> "No hay ingresos registrados esta semana."
            r.diasAsistencia == 1 -> "$nombre asistió 1 día" + (r.entradaMasTemprana?.let { " (entrada $it)." } ?: ".")
            else -> "$nombre asistió ${r.diasAsistencia} días" +
                (r.entradaMasTemprana?.let { "; la entrada más temprana fue a las $it." } ?: ".")
        }
        if (r.comidasRegistradas > 0) {
            val pct = r.comidasCompletas * 100 / r.comidasRegistradas
            partes += when {
                pct >= 75 -> "Comió bien: terminó ${r.comidasCompletas} de ${r.comidasRegistradas} comidas."
                pct >= 40 -> "En alimentación fue irregular: terminó ${r.comidasCompletas} de ${r.comidasRegistradas} comidas."
                else -> "Comió poco: solo terminó ${r.comidasCompletas} de ${r.comidasRegistradas} comidas."
            }
        }
        if (r.diasConSiesta > 0) {
            partes += "Durmió siesta ${r.diasConSiesta} día(s), en promedio ${formatoDuracion(r.minutosSiesta / r.diasConSiesta)}."
        }
        r.animoFrecuente?.let { partes += "Su estado de ánimo más frecuente fue \"$it\"." }
        return partes.joinToString(" ")
    }

    // ---- utilidades ----

    /** Acepta dd/MM/yyyy (bitácora) y yyyy-MM-dd (asistencia del servidor). */
    internal fun parseFecha(texto: String?): Date? {
        val t = texto?.trim().orEmpty()
        if (t.isEmpty()) return null
        val formato = when {
            Regex("\\d{4}-\\d{2}-\\d{2}.*").matches(t) -> "yyyy-MM-dd"
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}").matches(t) -> "dd/MM/yyyy"
            else -> return null
        }
        return try {
            SimpleDateFormat(formato, Locale.US).apply { isLenient = false }.parse(t.take(10))
        } catch (_: Exception) {
            null
        }
    }

    private fun enSemana(fecha: Date?, s: SemanaEscolar): Boolean {
        if (fecha == null) return false
        val finDomingo = Calendar.getInstance().apply { time = s.lunes; add(Calendar.DAY_OF_MONTH, 7) }.time
        return !fecha.before(s.lunes) && fecha.before(finDomingo)
    }

    private fun claveDia(d: Date): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(d)

    internal fun minutosDelDia(hora: String): Int? {
        val t = hora.trim()
        for (f in listOf("hh:mm a", "HH:mm")) {
            try {
                val d = SimpleDateFormat(f, Locale.US).apply { isLenient = false }.parse(t) ?: continue
                val c = Calendar.getInstance().apply { time = d }
                return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
            } catch (_: Exception) {
            }
        }
        return null
    }

    /** Quita el emoji del badge ("🙂 Feliz" → "Feliz"). */
    private fun limpiarAnimo(texto: String): String? =
        texto.replace(Regex("[^\\p{L}\\p{N} ]"), "").trim().takeIf { it.isNotEmpty() }

    fun formatoDuracion(minutos: Int): String {
        val h = minutos / 60
        val m = minutos % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

/** Lo que muestra la pantalla del resumen: el del servidor si existe para esa semana; si no, el cálculo local. */
data class ResumenVista(
    val semana: SemanaEscolar,
    val texto: String,
    val asistenciaTexto: String,
    val asistenciaDetalle: String,
    val alimentacionTexto: String,
    val alimentacionDetalle: String,
    val descansoTexto: String,
    val descansoDetalle: String,
    val animoTexto: String,
    val animoDetalle: String,
    val delServidor: Boolean
)

fun ResumenSemanal.aVista() = ResumenVista(
    semana = semana,
    texto = texto,
    asistenciaTexto = asistenciaTexto,
    asistenciaDetalle = asistenciaDetalle,
    alimentacionTexto = alimentacionTexto,
    alimentacionDetalle = alimentacionDetalle,
    descansoTexto = descansoTexto,
    descansoDetalle = descansoDetalle,
    animoTexto = animoTexto,
    animoDetalle = animoDetalle,
    delServidor = false
)

/** Id con el que el servidor identifica la semana: el lunes en formato YYYY-MM-DD. */
fun SemanaEscolar.idServidor(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(lunes)

/**
 * Usa el resumen semanal calculado por el servidor (el mismo que ve la familia en su inicio)
 * para que ambos coincidan; si aún no llegó o no hay conexión, usa el cálculo local.
 */
fun vistaResumen(local: ResumenSemanal, delServidor: List<WeeklySummary>): ResumenVista {
    val s = delServidor.firstOrNull { it.id == local.semana.idServidor() } ?: return local.aVista()
    fun dato(valor: String, siNoHay: String) = valor.trim().takeUnless { it.isEmpty() || it == "-" } ?: siNoHay
    val llegada = dato(s.averageArrivalTime, "")
    return ResumenVista(
        semana = local.semana,
        texto = dato(s.executiveSummary, local.texto),
        asistenciaTexto = "${s.attendanceDaysCount}/${s.totalSchoolDays} días",
        asistenciaDetalle = if (llegada.isNotEmpty()) "Llegada promedio $llegada" else "Sin ingresos registrados",
        alimentacionTexto = dato(s.foodIntakePercentage, "Sin datos"),
        alimentacionDetalle = s.foodHighlights.firstOrNull { it.isNotBlank() } ?: "Sin comidas registradas",
        descansoTexto = dato(s.totalNapHours, "Sin datos"),
        descansoDetalle = dato(s.averageNapDaily, "Sin siestas registradas"),
        animoTexto = dato(s.moodSummary, "Sin datos"),
        animoDetalle = "Comportamiento semanal",
        delServidor = true
    )
}
