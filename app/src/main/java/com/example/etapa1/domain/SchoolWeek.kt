package com.example.etapa1.domain

import java.util.Calendar
import java.util.Locale

/** Días de clase (lunes a viernes) y textos de fecha para la planeación semanal. */
object SchoolWeek {

    private val SHORT_DAYS = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

    fun plusDays(date: SimpleDate, days: Int): SimpleDate {
        val calendar = date.toCalendar()
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return SimpleDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** 1 = domingo ... 7 = sábado (igual que [Calendar.DAY_OF_WEEK]). */
    fun dayOfWeek(date: SimpleDate): Int = date.toCalendar().get(Calendar.DAY_OF_WEEK)

    fun isSchoolDay(date: SimpleDate): Boolean = dayOfWeek(date) in Calendar.MONDAY..Calendar.FRIDAY

    /** Hoy si es día de clase; si es fin de semana, el lunes siguiente. */
    fun nextSchoolDay(today: SimpleDate): SimpleDate {
        var date = today
        while (!isSchoolDay(date)) date = plusDays(date, 1)
        return date
    }

    /** Lunes a viernes de la semana de clases actual (en fin de semana, la siguiente). */
    fun schoolWeek(today: SimpleDate): List<SimpleDate> {
        val reference = nextSchoolDay(today)
        val monday = plusDays(reference, -(dayOfWeek(reference) - Calendar.MONDAY))
        return (0..4).map { plusDays(monday, it) }
    }

    /**
     * Lunes a viernes de la semana que contiene [date]. A diferencia de [schoolWeek],
     * en sábado o domingo devuelve la semana que acaba de terminar (útil para reportes).
     */
    fun weekOf(date: SimpleDate): List<SimpleDate> {
        val day = dayOfWeek(date)
        val daysSinceMonday = if (day == Calendar.SUNDAY) 6 else day - Calendar.MONDAY
        val monday = plusDays(date, -daysSinceMonday)
        return (0..4).map { plusDays(monday, it) }
    }

    fun isAfter(a: SimpleDate, b: SimpleDate): Boolean =
        compareValuesBy(a, b, { it.year }, { it.month }, { it.day }) > 0

    /** Ej. "05/10/2026". */
    fun numericLabel(date: SimpleDate): String =
        String.format(Locale.US, "%02d/%02d/%04d", date.day, date.month, date.year)

    /** Ej. "Lun 5". */
    fun shortLabel(date: SimpleDate): String = "${SHORT_DAYS[dayOfWeek(date) - 1]} ${date.day}"
}
