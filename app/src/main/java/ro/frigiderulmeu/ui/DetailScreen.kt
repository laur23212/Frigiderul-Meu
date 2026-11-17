package ro.frigiderulmeu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.frigiderulmeu.R
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.domain.CalendarMonth
import ro.frigiderulmeu.domain.Expiry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: FridgeViewModel,
    itemId: Long,
    onBack: () -> Unit,
    onConsumed: (Item) -> Unit,
) {
    BackHandler(onBack = onBack)
    val itemFlow = remember(itemId) { vm.observeItem(itemId) }
    val item by itemFlow.collectAsStateWithLifecycle(initialValue = null)
    val today by vm.today.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Produs") },
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi") }
            }
        )
        val current = item
        if (current != null) {
            DetailContent(vm, current, today, onBack, onConsumed)
        }
    }
}

@Composable
private fun DetailContent(
    vm: FridgeViewModel,
    item: Item,
    today: java.time.LocalDate,
    onBack: () -> Unit,
    onConsumed: (Item) -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var quantity by rememberSaveable(item.id) { mutableStateOf(item.quantity.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val changed = name.trim() != item.name || quantity.trim() != item.quantity.orEmpty()
    val state = Expiry.stateOf(item.expiryDate, today)

    val photos = rememberPhotoLaunchers(vm, onBusy = { busy = it }, onPhoto = { vm.changePhoto(item, it) })

    // Dacă numele se schimbă din altă parte, sincronizăm câmpul doar când nu e editat.
    LaunchedEffect(item.name, item.quantity) {
        if (!changed) {
            name = item.name
            quantity = item.quantity.orEmpty()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PhotoBox(
            item.imagePath, item.name, busy,
            Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = photos.takePhoto) {
                Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp)); Text("Fotografiază")
            }
            OutlinedButton(onClick = photos.pickFromGallery) {
                Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp)); Text("Din galerie")
            }
        }

        OutlinedTextField(name, { name = it }, label = { Text("Nume") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            quantity, { quantity = it },
            label = { Text("Cantitate (opțional)") },
            placeholder = { Text("ex. 1 L, 2 buc.") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (changed) {
            Button(
                onClick = { vm.update(item.copy(name = name.trim(), quantity = quantity.trim().ifEmpty { null })) },
                enabled = name.isNotBlank()
            ) { Text("Salvează modificările") }
        }

        // Data expirării
        Column {
            Text("Data expirării", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(CalendarMonth.longDate(item.expiryDate), style = MaterialTheme.typography.titleMedium)
                    Text(Expiry.label(item.expiryDate, today), color = StatusColors.text(state), fontWeight = FontWeight.Medium)
                }
                OutlinedButton(onClick = { showDatePicker = true }) { Text("Modifică") }
            }
        }

        // Deschis / nedeschis
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Marchează ca deschis", style = MaterialTheme.typography.titleMedium)
                val opened = item.openedDate
                Text(
                    when {
                        item.isOpened && opened != null -> "Deschis pe ${CalendarMonth.longDate(opened)}"
                        item.isOpened -> "Deschis"
                        else -> "Nedeschis"
                    },
                    color = if (item.isOpened) StatusColors.opened else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = item.isOpened, onCheckedChange = { vm.setOpened(item, it) })
        }

        Text(
            "Adăugat pe ${CalendarMonth.longDate(item.addedDate)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.size(4.dp))
        Button(
            onClick = { vm.consume(item); onConsumed(item); onBack() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Consumat / scos din frigider") }
        OutlinedButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Șterge produsul") }
    }

    if (showDatePicker) {
        DatePickerPopup(item.expiryDate, onDismiss = { showDatePicker = false }) {
            vm.update(item.copy(expiryDate = it))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Ștergi produsul?") },
            text = { Text("„${item.name}” va fi șters definitiv.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(item); onBack() }) {
                    Text("Șterge", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Renunță") } }
        )
    }
}
