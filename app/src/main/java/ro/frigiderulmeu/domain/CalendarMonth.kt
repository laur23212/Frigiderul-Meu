package ro.frigiderulmeu.domain

import java.time.LocalDate
import java.time.YearMonth

object CalendarMonth {
    private val MONTHS = listOf(
        "ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
        "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"
    )
    private val MONTHS_SHORT = listOf(
        "ian.", "feb.", "mar.", "apr.", "mai", "iun.",
        "iul.", "aug.", "sept.", "oct.", "nov.", "dec."
    )
    val WEEKDAYS = listOf("L", "Ma", "Mi", "J", "V", "S", "D")

    /**
     * Căsuțele unei luni, în rânduri de câte 7, cu săptămâna începând luni.
     * Zilele din afara lunii sunt null.
     */
    fun cells(month: YearMonth): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = first.dayOfWeek.value - 1 // luni = 1 -> 0 căsuțe goale
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val all = List<LocalDate?>(leading) { null } + days
        val trailing = (7 - all.size % 7) % 7
        return all + List<LocalDate?>(trailing) { null }
    }

    fun title(month: YearMonth): String =
        MONTHS[month.monthValue - 1].replaceFirstChar { it.uppercase() } + " " + month.year

    /** De exemplu „21 nov.” sau „21 nov. 2027” dacă anul diferă de cel curent. */
    fun shortDate(date: LocalDate, today: LocalDate): String {
        val base = "${date.dayOfMonth} ${MONTHS_SHORT[date.monthValue - 1]}"
        return if (date.year == today.year) base else "$base ${date.year}"
    }

    /** De exemplu „21 noiembrie 2026”. */
    fun longDate(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]} ${date.year}"
}
