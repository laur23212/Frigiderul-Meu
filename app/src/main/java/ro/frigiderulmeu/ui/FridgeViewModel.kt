package ro.frigiderulmeu.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.frigiderulmeu.data.AppDatabase
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.data.ProductType
import ro.frigiderulmeu.domain.ItemActions
import ro.frigiderulmeu.domain.Today
import ro.frigiderulmeu.image.ImageStore
import java.io.File
import java.time.LocalDate

class FridgeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val itemDao = db.itemDao()
    private val typeDao = db.productTypeDao()
    val images = ImageStore.get(app)
    private val prefs = app.getSharedPreferences("setari", Context.MODE_PRIVATE)

    val today: StateFlow<LocalDate> = Today.date

    /** Exemplarele active (neconsumate), sortate după data expirării. */
    val activeItems: StateFlow<List<Item>> = itemDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Exemplarele active grupate pe ziua expirării, pentru calendar. */
    val itemsByDate: StateFlow<Map<LocalDate, List<Item>>> = activeItems
        .map { ItemActions.groupByExpiry(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    var calendarExpanded by mutableStateOf(prefs.getBoolean(KEY_EXPANDED, false))
        private set

    var sortByAdded by mutableStateOf(prefs.getBoolean(KEY_SORT_ADDED, false))
        private set

    init {
        // Curăță fotografiile temporare rămase de la formulare abandonate.
        viewModelScope.launch(Dispatchers.IO) {
            val limit = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            File(app.cacheDir, "pending").listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
        }
    }

    fun toggleCalendarMode() {
        calendarExpanded = !calendarExpanded
        prefs.edit().putBoolean(KEY_EXPANDED, calendarExpanded).apply()
    }

    fun setSortByAdded(value: Boolean) {
        sortByAdded = value
        prefs.edit().putBoolean(KEY_SORT_ADDED, value).apply()
    }

    fun observeItem(id: Long): Flow<Item?> = itemDao.observeById(id)

    /** Redimensionează și comprimă fotografia; întoarce calea fișierului temporar. */
    suspend fun processPhoto(uri: Uri): String? = images.processToPending(uri)?.absolutePath

    /**
     * Salvează un exemplar nou. [onSaved] primește calea definitivă a fotografiei,
     * ca să poată fi refolosită la „Salvează și adaugă încă unul la fel”.
     */
    fun addItem(
        name: String,
        expiry: LocalDate,
        quantity: String?,
        opened: Boolean,
        photoPath: String?,
        barcode: String? = null,
        onSaved: (finalPhotoPath: String?) -> Unit = {},
    ) {
        viewModelScope.launch {
            val today = Today.date.value
            val finalPath = withContext(Dispatchers.IO) { images.commit(photoPath) }
            val item = Item(
                name = name.trim(),
                barcode = barcode,
                imagePath = finalPath,
                expiryDate = expiry,
                addedDate = today,
                quantity = quantity?.trim()?.ifEmpty { null },
                isOpened = opened,
                openedDate = if (opened) today else null,
            )
            itemDao.insert(item)
            if (barcode != null) rememberProductType(barcode, item.name, finalPath)
            onSaved(finalPath)
        }
    }

    /** Pregătit pentru scanarea codurilor de bare (etapa 2): memorează numele și poza pentru cod. */
    private suspend fun rememberProductType(barcode: String, name: String, imagePath: String?) {
        val existing = typeDao.findByBarcode(barcode)
        typeDao.insertOrReplace(
            ProductType(
                id = existing?.id ?: 0,
                barcode = barcode,
                name = name,
                imagePath = imagePath ?: existing?.imagePath,
                daysValidAfterOpening = existing?.daysValidAfterOpening,
            )
        )
    }

    fun update(item: Item) {
        viewModelScope.launch { itemDao.update(item) }
    }

    fun setOpened(item: Item, opened: Boolean) {
        val updated = if (opened) ItemActions.markOpened(item, Today.date.value) else ItemActions.markUnopened(item)
        update(updated)
    }

    fun changePhoto(item: Item, pendingPath: String) {
        viewModelScope.launch {
            val finalPath = withContext(Dispatchers.IO) { images.commit(pendingPath) }
            itemDao.update(item.copy(imagePath = finalPath))
            cleanupImage(item.imagePath)
        }
    }

    fun consume(item: Item) = update(ItemActions.markConsumed(item, Today.date.value))

    fun restore(item: Item) = update(ItemActions.restore(item))

    fun delete(item: Item) {
        viewModelScope.launch {
            itemDao.delete(item)
            cleanupImage(item.imagePath)
        }
    }

    /** Șterge fișierul imagine doar dacă nu mai este folosit de alt exemplar sau tip de produs. */
    private suspend fun cleanupImage(path: String?) {
        if (path == null) return
        if (itemDao.countWithImage(path) == 0 && typeDao.countWithImage(path) == 0) {
            withContext(Dispatchers.IO) { images.delete(path) }
        }
    }

    private companion object {
        const val KEY_EXPANDED = "calendar_extins"
        const val KEY_SORT_ADDED = "sortare_dupa_adaugare"
    }
}
