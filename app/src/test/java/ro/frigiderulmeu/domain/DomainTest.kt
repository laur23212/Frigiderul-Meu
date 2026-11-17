package ro.frigiderulmeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.data.ItemStatus
import ro.frigiderulmeu.data.StorageType
import ro.frigiderulmeu.image.ImageStore
import java.time.LocalDate
import java.time.YearMonth

class ExpiryTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test fun greenWhenMoreThanTwoDays() {
        assertEquals(ExpiryState.GREEN, Expiry.stateOf(today.plusDays(3), today))
        assertEquals(ExpiryState.GREEN, Expiry.stateOf(today.plusDays(40), today))
    }

    @Test fun orangeForTodayTomorrowAndTwoDays() {
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(today, today))
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(today.plusDays(1), today))
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(today.plusDays(2), today))
    }

    @Test fun redWhenPast() {
        assertEquals(ExpiryState.RED, Expiry.stateOf(today.minusDays(1), today))
        assertEquals(ExpiryState.RED, Expiry.stateOf(today.minusDays(100), today))
    }

    @Test fun stateChangesWhenDayChanges() {
        val expiry = LocalDate.of(2026, 11, 20)
        assertEquals(ExpiryState.GREEN, Expiry.stateOf(expiry, LocalDate.of(2026, 11, 17)))
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(expiry, LocalDate.of(2026, 11, 18)))
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(expiry, LocalDate.of(2026, 11, 20)))
        assertEquals(ExpiryState.RED, Expiry.stateOf(expiry, LocalDate.of(2026, 11, 21)))
    }

    @Test fun acrossMonthAndYearBoundaries() {
        assertEquals(ExpiryState.ORANGE, Expiry.stateOf(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 12, 30)))
        assertEquals(ExpiryState.GREEN, Expiry.stateOf(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 11, 29)))
        assertEquals(ExpiryState.RED, Expiry.stateOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 1)))
    }

    @Test fun labels() {
        assertEquals("Expiră astăzi", Expiry.label(today, today))
        assertEquals("Expiră mâine", Expiry.label(today.plusDays(1), today))
        assertEquals("Expiră în 2 zile", Expiry.label(today.plusDays(2), today))
        assertEquals("Expiră în 25 de zile", Expiry.label(today.plusDays(25), today))
        assertEquals("Expirat ieri", Expiry.label(today.minusDays(1), today))
        assertEquals("Expirat de 3 zile", Expiry.label(today.minusDays(3), today))
    }

    @Test fun worstStateOfDay() {
        val items = listOf(item(today.plusDays(5)), item(today.minusDays(1)))
        assertEquals(ExpiryState.RED, Expiry.worstState(items, today))
        assertNull(Expiry.worstState(emptyList(), today))
    }
}

class ItemActionsTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test fun openingDoesNotChangeExpiryOrStatus() {
        val original = item(today.plusDays(7))
        val opened = ItemActions.markOpened(original, today)
        assertTrue(opened.isOpened)
        assertEquals(today, opened.openedDate)
        assertEquals(original.expiryDate, opened.expiryDate)
        assertEquals(ItemStatus.ACTIVE, opened.status)
    }

    @Test fun openedKeepsItsExpiryColor() {
        val opened = ItemActions.markOpened(item(today.minusDays(2)), today)
        assertEquals(ExpiryState.RED, Expiry.stateOf(opened.expiryDate, today))
        assertTrue(opened.isOpened)
    }

    @Test fun markOpenedTwiceKeepsFirstDate() {
        val first = ItemActions.markOpened(item(today), today.minusDays(3))
        val again = ItemActions.markOpened(first, today)
        assertEquals(today.minusDays(3), again.openedDate)
    }

    @Test fun unopenUndoesAccidentalTap() {
        val undone = ItemActions.markUnopened(ItemActions.markOpened(item(today), today))
        assertFalse(undone.isOpened)
        assertNull(undone.openedDate)
    }

    @Test fun consumedItemsLeaveTheCalendar() {
        val milk = item(today.plusDays(1), id = 1)
        val yogurt = item(today.plusDays(1), id = 2)
        val consumed = ItemActions.markConsumed(milk, today)
        assertEquals(ItemStatus.CONSUMED, consumed.status)
        assertEquals(today, consumed.consumedDate)
        val grouped = ItemActions.groupByExpiry(listOf(consumed, yogurt))
        assertEquals(listOf(yogurt), grouped[today.plusDays(1)])
    }

    @Test fun restoreAfterUndo() {
        val restored = ItemActions.restore(ItemActions.markConsumed(item(today), today))
        assertEquals(ItemStatus.ACTIVE, restored.status)
        assertNull(restored.consumedDate)
    }

    @Test fun severalItemsOnSameDayAreGroupedTogether() {
        val day = LocalDate.of(2026, 11, 10)
        val grouped = ItemActions.groupByExpiry(listOf(item(day, 1), item(day, 2), item(day, 3), item(day.plusDays(1), 4)))
        assertEquals(3, grouped[day]?.size)
        assertEquals(1, grouped[day.plusDays(1)]?.size)
    }
}

class OpenSplitTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test fun openingOneOfSeveralCreatesSeparateEntryExpiringInThreeDays() {
        val pack = item(today.plusDays(10)).copy(id = 5, pieces = 3)
        val r = ItemActions.open(pack, today)
        assertEquals(2, r.updated.pieces)
        assertFalse(r.updated.isOpened)
        assertEquals(pack.expiryDate, r.updated.expiryDate)
        val opened = r.split!!
        assertEquals(0L, opened.id)
        assertEquals(1, opened.pieces)
        assertTrue(opened.isOpened)
        assertEquals(today, opened.openedDate)
        assertEquals(today.plusDays(3), opened.expiryDate)
    }

    @Test fun openedPieceNeverLivesLongerThanTheOriginalDate() {
        val pack = item(today.plusDays(1)).copy(pieces = 2)
        assertEquals(today.plusDays(1), ItemActions.open(pack, today).split!!.expiryDate)
    }

    @Test fun freezerAndPantryKeepTheirDate() {
        listOf(StorageType.FREEZER, StorageType.PANTRY).forEach { type ->
            val pack = item(today.plusDays(90)).copy(pieces = 2, storage = type)
            val opened = ItemActions.open(pack, today).split!!
            assertEquals(today.plusDays(90), opened.expiryDate)
            assertEquals(type, opened.storage)
            assertTrue(opened.isOpened)
        }
    }

    @Test fun singlePieceFromFridgeAlsoExpiresInThreeDays() {
        val r = ItemActions.open(item(today.plusDays(10)), today)
        assertNull(r.split)
        assertTrue(r.updated.isOpened)
        assertEquals(today, r.updated.openedDate)
        assertEquals(today.plusDays(3), r.updated.expiryDate)
    }

    @Test fun singleFrozenPieceKeepsItsDateWhenOpened() {
        val r = ItemActions.open(item(today.plusDays(90)).copy(storage = StorageType.FREEZER), today)
        assertTrue(r.updated.isOpened)
        assertEquals(today.plusDays(90), r.updated.expiryDate)
    }

    @Test fun consumeOneKeepsTheRestThenRemovesTheLast() {
        val pack = item(today).copy(pieces = 2)
        val one = ItemActions.consumeOne(pack, today)
        assertEquals(1, one.pieces)
        assertEquals(ItemStatus.ACTIVE, one.status)
        assertEquals(ItemStatus.CONSUMED, ItemActions.consumeOne(one, today).status)
    }

    @Test fun alreadyOpenedStaysUntouched() {
        val opened = ItemActions.markOpened(item(today).copy(pieces = 2), today)
        val r = ItemActions.open(opened, today)
        assertNull(r.split)
        assertEquals(opened, r.updated)
    }
}

class CropRectTest {
    @Test fun noZoomNoMoveTakesCenteredShortSideSquare() {
        assertEquals(Triple(100, 0, 600), cropRect(800, 600, 300f, 1f, 0f, 0f))
    }

    @Test fun zoomTwoHalvesTheSquareAndMovingShiftsIt() {
        // zoom 2 -> latura 300; deplasând poza spre dreapta cu 150 px de ecran, se vede mai mult din stânga.
        assertEquals(Triple(250, 150, 300), cropRect(800, 600, 300f, 2f, 0f, 0f))
        assertEquals(Triple(100, 150, 300), cropRect(800, 600, 300f, 2f, 150f, 0f))
    }

    @Test fun neverLeavesThePhoto() {
        val r = cropRect(800, 600, 300f, 2f, 99999f, -99999f)
        assertEquals(0, r.first)
        assertEquals(300, r.second)
    }
}

class CalendarMonthTest {
    @Test fun weekStartsOnMonday() {
        // 1 noiembrie 2026 este duminică -> 6 căsuțe goale înainte.
        val cells = CalendarMonth.cells(YearMonth.of(2026, 11))
        assertEquals(6, cells.takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(0, cells.size % 7)
    }

    @Test fun monthStartingOnMondayHasNoLeadingBlanks() {
        // 1 iunie 2026 este luni.
        val cells = CalendarMonth.cells(YearMonth.of(2026, 6))
        assertEquals(LocalDate.of(2026, 6, 1), cells[0])
    }

    @Test fun containsEveryDayIncludingLeapDay() {
        val cells = CalendarMonth.cells(YearMonth.of(2028, 2)).filterNotNull()
        assertEquals(29, cells.size)
        assertEquals(LocalDate.of(2028, 2, 29), cells.last())
    }

    @Test fun monthNavigationAcrossYears() {
        assertEquals(YearMonth.of(2027, 1), YearMonth.of(2026, 12).plusMonths(1))
        assertEquals("Ianuarie 2027", CalendarMonth.title(YearMonth.of(2027, 1)))
        assertEquals("Noiembrie 2026", CalendarMonth.title(YearMonth.of(2026, 11)))
    }

    @Test fun shortDates() {
        val today = LocalDate.of(2026, 11, 17)
        assertEquals("21 nov. 2026", CalendarMonth.shortDate(LocalDate.of(2026, 11, 21), today))
        assertEquals("3 ian. 2027", CalendarMonth.shortDate(LocalDate.of(2027, 1, 3), today))
    }

    @Test fun imageTargetSize() {
        assertEquals(400 to 300, ImageStore.targetSize(4000, 3000))
        assertEquals(300 to 400, ImageStore.targetSize(3000, 4000))
        assertEquals(200 to 100, ImageStore.targetSize(200, 100))
    }
}

private fun item(expiry: LocalDate, id: Long = 0) = Item(
    id = id,
    name = "Lapte",
    expiryDate = expiry,
    addedDate = LocalDate.of(2026, 11, 1),
)
