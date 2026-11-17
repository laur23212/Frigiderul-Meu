package ro.frigiderulmeu.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.data.StorageType
import ro.frigiderulmeu.domain.Expiry
import ro.frigiderulmeu.image.ProductImage
import java.time.LocalDate

/** Inventarul, ca o galerie foto: grilă de ~3 coloane, adaptată lățimii ecranului. */
@Composable
fun FridgeScreen(vm: FridgeViewModel, onOpenItem: (Long) -> Unit, onConsume: (Item) -> Unit) {
    val allItems by vm.activeItems.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val sortByAdded = vm.sortByAdded
    // null = „Toate” (implicit: se văd toate produsele); altfel doar categoria aleasă.
    var filter by rememberSaveable { mutableStateOf<StorageType?>(null) }
    val items = remember(allItems, filter) { if (filter == null) allItems else allItems.filter { it.storage == filter } }
    val sorted = remember(items, sortByAdded) {
        if (sortByAdded) items.sortedWith(compareByDescending<Item> { it.addedDate }.thenByDescending { it.id })
        else items // deja sortate după data expirării, din baza de date
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Frigiderul meu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (items.size == 1) "1 produs" else "${items.size} produse",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Toate (${allItems.size})") })
            StorageType.values().forEach { type ->
                val count = allItems.count { it.storage == type }
                FilterChip(selected = filter == type, onClick = { filter = type }, label = { Text("${storageLabel(type)} ($count)") })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !sortByAdded, onClick = { vm.chooseSortByAdded(false) }, label = { Text("După expirare") })
            FilterChip(selected = sortByAdded, onClick = { vm.chooseSortByAdded(true) }, label = { Text("Adăugate recent") })
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (filter == null) "Frigiderul e gol.\nApasă „Adaugă” ca să pui primul produs."
                    else "Nu ai produse la „${storageLabel(filter!!)}”.\nBifează locul de păstrare când adaugi un produs.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 106.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sorted, key = { it.id }) { item ->
                    InventoryCard(item, today, onConsume = { onConsume(item) }) { onOpenItem(item.id) }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(item: Item, today: LocalDate, onConsume: () -> Unit, onClick: () -> Unit) {
    val state = Expiry.stateOf(item.expiryDate, today)
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .border(3.dp, StatusColors.border(state), shape)
        ) {
            ProductImage(item.imagePath, item.name, Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(11.dp)))
            if (item.isOpened) OpenedTag(Modifier.align(Alignment.TopStart).padding(7.dp))
            if (item.storage != StorageType.FRIDGE) StorageTag(item.storage, Modifier.align(Alignment.BottomStart).padding(7.dp))
            if (item.pieces > 1) PiecesTag(item.pieces, Modifier.align(Alignment.TopEnd).padding(7.dp))
            ConsumeButton(item, onConsume, Modifier.align(Alignment.BottomEnd).padding(5.dp).size(34.dp))
        }
        Text(
            item.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
        )
        Row(Modifier.padding(start = 2.dp)) {
            Text(shortDate(item.expiryDate, today), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            Expiry.label(item.expiryDate, today),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = StatusColors.text(state),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
