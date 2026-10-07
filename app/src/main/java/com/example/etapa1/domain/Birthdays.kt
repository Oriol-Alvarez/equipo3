package com.example.etapa1.domain

import java.util.Calendar
import java.util.GregorianCalendar

/** Fecha simple (día, mes 1-12, año). Se usa en lugar de java.time porque la app soporta desde API 24. */
data class SimpleDate(val year: Int, val month: Int, val day: Int) {

    fun toCalendar(): Calendar = GregorianCalendar(year, month - 1, day)

    companion object {
        /** Convierte "dd/MM/yyyy" (formato de la ficha del niño). Devuelve null si no es válida. */
        fun parse(text: String): SimpleDate? {
            val parts = text.trim().split("/")
            if (parts.size != 3) return null
            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val year = parts[2].toIntOrNull() ?: return null
            if (month !in 1..12 || day !in 1..31) return null
            val calendar = GregorianCalendar(year, month - 1, 1)
            if (day > calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) return null
            return SimpleDate(year, month, day)
        }

        fun today(): SimpleDate {
            val now = Calendar.getInstance()
            return SimpleDate(
                year = now.get(Calendar.YEAR),
                month = now.get(Calendar.MONTH) + 1,
                day = now.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}

data class UpcomingBirthday(
    val childId: String,
    val childName: String,
    val date: SimpleDate,
    /** 0 = hoy, 1 = mañana, ... */
    val daysUntil: Int,
    val turningAge: Int
) {
    val isToday: Boolean get() = daysUntil == 0
}

/** Reglas de cumpleaños, sin dependencias de Android para poder probarlas. */
object BirthdayCalculator {

    private val MONTHS = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    private val WEEKDAYS = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

    /** Próximo cumpleaños a partir de [today] (puede ser hoy). El 29 de febrero se celebra el 28 en años no bisiestos. */
    fun nextBirthday(birthDate: SimpleDate, today: SimpleDate): SimpleDate {
        val thisYear = birthdayInYear(birthDate, today.year)
        return if (compare(thisYear, today) >= 0) thisYear else birthdayInYear(birthDate, today.year + 1)
    }

    fun daysBetween(from: SimpleDate, to: SimpleDate): Int {
        val millis = to.toCalendar().timeInMillis - from.toCalendar().timeInMillis
        // Se redondea para que el cambio de horario de verano no reste un día.
        return Math.round(millis / 86_400_000.0).toInt()
    }

    fun upcoming(
        birthDate: SimpleDate,
        childId: String,
        childName: String,
        today: SimpleDate
    ): UpcomingBirthday {
        val next = nextBirthday(birthDate, today)
        return UpcomingBirthday(
            childId = childId,
            childName = childName,
            date = next,
            daysUntil = daysBetween(today, next),
            turningAge = next.year - birthDate.year
        )
    }

    /** Cumpleaños entre hoy y los próximos [days] días, ordenados del más cercano al más lejano. */
    fun upcomingWithin(birthdays: List<UpcomingBirthday>, days: Int): List<UpcomingBirthday> =
        birthdays.filter { it.daysUntil in 0..days }.sortedWith(compareBy({ it.daysUntil }, { it.childName }))

    /** Ej. "jueves 9 de octubre". */
    fun formatDate(date: SimpleDate): String {
        val weekday = WEEKDAYS[date.toCalendar().get(Calendar.DAY_OF_WEEK) - 1]
        return "$weekday ${date.day} de ${MONTHS[date.month - 1]}"
    }

    fun whenText(daysUntil: Int): String = when (daysUntil) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "En $daysUntil días"
    }

    fun ageText(age: Int): String = if (age == 1) "1 año" else "$age años"

    private fun birthdayInYear(birthDate: SimpleDate, year: Int): SimpleDate {
        val isLeap = GregorianCalendar().isLeapYear(year)
        return if (birthDate.month == 2 && birthDate.day == 29 && !isLeap) {
            SimpleDate(year, 2, 28)
        } else {
            SimpleDate(year, birthDate.month, birthDate.day)
        }
    }

    private fun compare(a: SimpleDate, b: SimpleDate): Int =
        compareValuesBy(a, b, { it.year }, { it.month }, { it.day })
}
