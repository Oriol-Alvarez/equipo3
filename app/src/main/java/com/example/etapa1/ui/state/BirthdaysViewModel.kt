package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.domain.BirthdayCalculator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.domain.UpcomingBirthday
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Avisos de cumpleaños: la educadora ve los de su sala (hoy y próximos días)
 * y la familia ve una felicitación cuando se acerca el cumpleaños de su hijo o hija.
 */
class BirthdaysViewModel(
    private val childRepository: ChildRepository,
    private val today: () -> SimpleDate = { SimpleDate.today() }
) : ViewModel() {

    /** Cumpleaños de la sala entre hoy y los próximos [WINDOW_DAYS] días. */
    val roomBirthdays: StateFlow<List<UpcomingBirthday>> = childRepository.childrenSala1A
        .map { children -> upcomingFor(children) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _familyNotice = MutableStateFlow<UpcomingBirthday?>(null)
    /** Aviso para la familia del niño seleccionado, o null si su cumpleaños no está cerca. */
    val familyNotice: StateFlow<UpcomingBirthday?> = _familyNotice

    fun selectFamilyChild(child: Child) {
        _familyNotice.value = upcomingFor(listOf(child)).firstOrNull()
    }

    private fun upcomingFor(children: List<Child>): List<UpcomingBirthday> {
        val currentDay = today()
        val birthdays = children.mapNotNull { child ->
            val birthDate = SimpleDate.parse(childRepository.getChildFullProfile(child).birthDate)
                ?: return@mapNotNull null
            BirthdayCalculator.upcoming(birthDate, child.id, child.fullName, currentDay)
        }
        return BirthdayCalculator.upcomingWithin(birthdays, WINDOW_DAYS)
    }

    companion object {
        const val WINDOW_DAYS = 7
    }
}
