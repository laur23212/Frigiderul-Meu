package ro.frigiderulmeu.domain

import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.data.ItemStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Starea de expirare, calculată față de data curentă a telefonului. Ordinea = gravitatea. */
enum class ExpiryState { GREEN, ORANGE, RED }

object Expiry {
    /** Pragul pentru portocaliu: azi, mâine sau peste exact 2 zile. */
    const val WARNING_DAYS = 2L

    fun daysUntil(expiry: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, expiry)

    fun stateOf(expiry: LocalDate, today: LocalDate): ExpiryState {
        val days = daysUntil(expiry, today)
        return when {
            days < 0 -> ExpiryState.RED
            days <= WARNING_DAYS -> ExpiryState.ORANGE
            else -> ExpiryState.GREEN
        }
    }

    /** Starea celei mai grave expirări dintr-o zi (pentru culoarea căsuței din calendar). */
    fun worstState(items: List<Item>, today: LocalDate): ExpiryState? =
        items.maxOfOrNull { stateOf(it.expiryDate, today) }

    /** Text care descrie starea, ca informația să nu depindă doar de culoare. */
    fun label(expiry: LocalDate, today: LocalDate): String {
        val d = daysUntil(expiry, today)
        return when {
            d < -1 -> "Expirat de ${days(-d)}"
            d == -1L -> "Expirat ieri"
            d == 0L -> "Expiră astăzi"
            d == 1L -> "Expiră mâine"
            else -> "Expiră în ${days(d)}"
        }
    }

    /** „5 zile”, dar „20 de zile” (regula numeralelor în română). */
    fun days(n: Long): String = if (n % 100 in 1L..19L) "$n zile" else "$n de zile"
}

/** Schimbările de stare ale unui exemplar, ca funcții pure (ușor de testat). */
object ItemActions {
    fun markOpened(item: Item, today: LocalDate): Item =
        if (item.isOpened) item else item.copy(isOpened = true, openedDate = today)

    /** Anulează marcajul „deschis” (de ex. apăsat din greșeală). */
    fun markUnopened(item: Item): Item = item.copy(isOpened = false, openedDate = null)

    fun markConsumed(item: Item, today: LocalDate): Item =
        item.copy(status = ItemStatus.CONSUMED, consumedDate = today)

    fun restore(item: Item): Item = item.copy(status = ItemStatus.ACTIVE, consumedDate = null)

    /** Exemplarele active, grupate pe ziua expirării (pentru calendar). Cele consumate nu apar. */
    fun groupByExpiry(items: List<Item>): Map<LocalDate, List<Item>> =
        items.filter { it.status == ItemStatus.ACTIVE }.groupBy { it.expiryDate }
}
