package com.example.etapa1.domain

import java.util.Calendar

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

    /** Ej. "Lun 5". */
    fun shortLabel(date: SimpleDate): String = "${SHORT_DAYS[dayOfWeek(date) - 1]} ${date.day}"
}
