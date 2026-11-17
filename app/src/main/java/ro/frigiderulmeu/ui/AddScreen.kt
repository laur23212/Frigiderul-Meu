package ro.frigiderulmeu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.frigiderulmeu.R
import ro.frigiderulmeu.domain.CalendarMonth
import ro.frigiderulmeu.domain.Expiry
import java.time.LocalDate

/**
 * Adăugare rapidă: fotografie (cameră sau galerie, opțională), nume, dată, cantitate, deschis.
 * [initialDate] vine din calendar („Adaugă un produs care expiră în această zi”).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    vm: FridgeViewModel,
    initialDate: LocalDate?,
    onSaved: (name: String, expiry: LocalDate) -> Unit,
) {
    val today by vm.today.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var expiryEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var opened by rememberSaveable { mutableStateOf(false) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val expiry = expiryEpochDay?.let(LocalDate::ofEpochDay)

    LaunchedEffect(initialDate) {
        if (initialDate != null) expiryEpochDay = initialDate.toEpochDay()
    }

    val photos = rememberPhotoLaunchers(vm, onBusy = { busy = it }, onPhoto = { photoPath = it })

    fun save(keepForAnother: Boolean) {
        val date = expiry ?: return
        val savedName = name.trim()
        vm.addItem(savedName, date, quantity, opened, photoPath) { finalPath ->
            if (keepForAnother) {
                // Același produs, altă dată de expirare: păstrăm numele, poza și cantitatea.
                photoPath = finalPath
                expiryEpochDay = null
                opened = false
            } else {
                name = ""; quantity = ""; opened = false; photoPath = null; expiryEpochDay = null
            }
            onSaved(savedName, date)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Adaugă produs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoBox(photoPath, name, busy, Modifier.size(132.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = photos.takePhoto) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp)); Text("Fotografiază")
                }
                OutlinedButton(onClick = photos.pickFromGallery) {
                    Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp)); Text("Din galerie")
                }
                if (photoPath != null) {
                    TextButton(onClick = { photoPath = null }) { Text("Fără fotografie") }
                }
            }
        }

        OutlinedTextField(
            name, { name = it },
            label = { Text("Nume produs") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Data expirării", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1L to "Mâine", 3L to "+3 zile", 7L to "+7 zile", 14L to "+14 zile", 30L to "+30 zile").forEach { (days, label) ->
                val date = today.plusDays(days)
                FilterChip(
                    selected = expiry == date,
                    onClick = { expiryEpochDay = date.toEpochDay() },
                    label = { Text(label) }
                )
            }
        }
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(expiry?.let { "${CalendarMonth.longDate(it)} · ${Expiry.label(it, today)}" } ?: "Alege data din calendar")
        }

        OutlinedTextField(
            quantity, { quantity = it },
            label = { Text("Cantitate (opțional)") },
            placeholder = { Text("ex. 1 L, 2 buc.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Este deja deschis", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = opened, onCheckedChange = { opened = it })
        }

        val canSave = name.isNotBlank() && expiry != null && !busy
        Button(onClick = { save(keepForAnother = false) }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text("Salvează")
        }
        OutlinedButton(onClick = { save(keepForAnother = true) }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text("Salvează și adaugă încă unul la fel")
        }
        if (!canSave && !busy) {
            Text(
                "Completează numele și data expirării.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDatePicker) {
        DatePickerPopup(expiry ?: today, onDismiss = { showDatePicker = false }) {
            expiryEpochDay = it.toEpochDay()
        }
    }
}
