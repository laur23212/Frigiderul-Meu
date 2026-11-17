package ro.frigiderulmeu.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ItemStatus { ACTIVE, CONSUMED }

/**
 * Un exemplar concret din frigider: are propria dată de expirare și propria stare de deschidere.
 * Numele și fotografia sunt copiate în exemplar, astfel încât editarea unui exemplar nu le schimbă pe celelalte.
 * Datele sunt [LocalDate] (zile calendaristice), salvate ca număr de zile, deci fusul orar nu le poate deplasa.
 */
@Entity(
    tableName = "items",
    indices = [Index("status", "expiryDate"), Index("barcode")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    /** Calea absolută către fișierul WebP din spațiul privat al aplicației, sau null. */
    val imagePath: String? = null,
    val expiryDate: LocalDate,
    val addedDate: LocalDate,
    val quantity: String? = null,
    val isOpened: Boolean = false,
    val openedDate: LocalDate? = null,
    val status: ItemStatus = ItemStatus.ACTIVE,
    val consumedDate: LocalDate? = null,
    /** Pregătit pentru o funcție viitoare „expiră la X zile după deschidere”. Nu este folosit automat. */
    @ColumnInfo(defaultValue = "NULL") val daysValidAfterOpening: Int? = null,
)

/**
 * Tipul de produs, identificat prin codul de bare: memorează numele și fotografia
 * ca să fie refolosite la următoarea scanare a aceluiași cod.
 */
@Entity(tableName = "product_types", indices = [Index(value = ["barcode"], unique = true)])
data class ProductType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val imagePath: String? = null,
    val daysValidAfterOpening: Int? = null,
)
