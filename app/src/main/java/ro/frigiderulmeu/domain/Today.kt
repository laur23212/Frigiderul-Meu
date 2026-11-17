package ro.frigiderulmeu.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Data curentă a telefonului, ca flux. Se reîmprospătează la fiecare revenire în aplicație
 * și la miezul nopții, astfel încât culorile se recalculează automat când se schimbă ziua.
 */
object Today {
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    fun refresh() {
        _date.value = LocalDate.now()
    }

    fun millisUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return ChronoUnit.MILLIS.between(now, midnight) + 1_000
    }
}
