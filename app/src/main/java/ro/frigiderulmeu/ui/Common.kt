package ro.frigiderulmeu.ui

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.data.StorageType
import ro.frigiderulmeu.domain.CalendarMonth
import ro.frigiderulmeu.domain.Expiry
import ro.frigiderulmeu.image.ProductImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Eticheta albastră „DESCHIS”, pentru inventar și liste. */
@Composable
fun OpenedTag(modifier: Modifier = Modifier) {
    Text(
        "DESCHIS",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StatusColors.opened)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Eticheta locului de păstrare („CONGELAT” / „LOC USCAT”); pentru frigider nu se afișează nimic. */
@Composable
fun StorageTag(storage: StorageType, modifier: Modifier = Modifier) {
    val (label, color) = when (storage) {
        StorageType.FREEZER -> "CONGELAT" to StatusColors.frozen
        StorageType.PANTRY -> "LOC USCAT" to StatusColors.pantry
        StorageType.FRIDGE -> return
    }
    Text(
        label,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Numele locului de păstrare, pentru butoane și filtre. */
fun storageLabel(storage: StorageType): String = when (storage) {
    StorageType.FRIDGE -> "Frigider"
    StorageType.FREEZER -> "Congelator"
    StorageType.PANTRY -> "Loc uscat"
}

/** Eticheta „×3” pentru intrările cu mai multe bucăți. */
@Composable
fun PiecesTag(count: Int, modifier: Modifier = Modifier) {
    Text(
        "×$count",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = "$count bucăți" }
    )
}

/** Indicatorul compact „deschis” de pe miniaturile din calendar: un cerc albastru cu „D”. */
@Composable
fun OpenedDot(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .padding(1.dp)
            .clip(CircleShape)
            .background(StatusColors.opened)
            .semantics { contentDescription = "deschis" },
        contentAlignment = Alignment.Center
    ) {
        if (size >= 12.dp) {
            Text("D", color = Color.White, fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.Bold, lineHeight = (size.value * 0.6f).sp)
        }
    }
}

/** Rând de listă pentru un produs: miniatură, nume, starea expirării, eticheta „deschis”. */
@Composable
fun ItemRow(item: Item, today: LocalDate, onConsume: (() -> Unit)? = null, onClick: () -> Unit) {
    val state = Expiry.stateOf(item.expiryDate, today)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StatusColors.background(state))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductImage(
            item.imagePath, item.name,
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, StatusColors.border(state), RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Expiry.label(item.expiryDate, today), color = StatusColors.text(state), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            item.quantity?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.isOpened) OpenedTag()
            StorageTag(item.storage)
            if (item.pieces > 1) PiecesTag(item.pieces)
        }
        if (onConsume != null) {
            Spacer(Modifier.width(8.dp))
            ConsumeButton(item, onConsume)
        }
    }
}

/** Buton rotund „consumat” (bifă): scoate o bucată sau tot produsul, cu „Anulează” în mesajul de jos. */
@Composable
fun ConsumeButton(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (item.pieces > 1) "Consumat o bucată din ${item.name}" else "Consumat: ${item.name}"
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .semantics { contentDescription = label }
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopup(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    // DatePicker lucrează în milisecunde UTC; conversia prin UTC evită decalajele de fus orar.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    ) {
        DatePicker(
            state = state,
            // Antetul arată mereu „zi lună an”, indiferent de limba telefonului.
            title = { Text("Alege data", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
            headline = {
                val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                Text(
                    picked?.let(CalendarMonth::longDate) ?: "Alege data",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                )
            },
            showModeToggle = false
        )
    }
}

/** Legenda culorilor, cu text, ca stările să nu depindă doar de culoare. */
@Composable
fun Legend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        LegendEntry(StatusColors.border(ro.frigiderulmeu.domain.ExpiryState.GREEN), "În termen")
        LegendEntry(StatusColors.border(ro.frigiderulmeu.domain.ExpiryState.ORANGE), "≤ 2 zile")
        LegendEntry(StatusColors.border(ro.frigiderulmeu.domain.ExpiryState.RED), "! Expirat")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OpenedDot(14.dp)
            Spacer(Modifier.width(4.dp))
            Text("Deschis", fontSize = 11.sp)
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

/** Lansatoarele pentru cameră și galerie; fotografia rezultată este deja redusă și comprimată. */
class PhotoLaunchers(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

@Composable
fun rememberPhotoLaunchers(
    vm: FridgeViewModel,
    onBusy: (Boolean) -> Unit,
    onPhoto: (String) -> Unit,
): PhotoLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }

    fun handle(uri: Uri) {
        scope.launch {
            onBusy(true)
            val bitmap = vm.loadForCrop(uri)
            onBusy(false)
            if (bitmap != null) cropSource = bitmap
            else Toast.makeText(context, "Fotografia nu a putut fi citită", Toast.LENGTH_SHORT).show()
        }
    }

    cropSource?.let { source ->
        CropDialog(
            source,
            onCancel = { cropSource = null },
            onDone = { left, top, size ->
                cropSource = null
                scope.launch {
                    onBusy(true)
                    val path = vm.saveCropped(source, left, top, size)
                    onBusy(false)
                    if (path != null) onPhoto(path)
                    else Toast.makeText(context, "Fotografia nu a putut fi salvată", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) handle(Uri.parse(uri))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) handle(uri)
    }

    return PhotoLaunchers(
        takePhoto = {
            val file = vm.images.newCameraFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri.toString()
            try {
                camera.launch(uri)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Nu am găsit o aplicație de cameră", Toast.LENGTH_SHORT).show()
            }
        },
        pickFromGallery = {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )
}

/** Fotografia mare din formular / pagina produsului. */
@Composable
fun PhotoBox(path: String?, name: String, busy: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            busy -> Text("Se procesează…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            path == null && name.isBlank() -> Text("Fără fotografie", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> ProductImage(path, name, Modifier.fillMaxSize())
        }
    }
}

fun shortDate(date: LocalDate, today: LocalDate) = CalendarMonth.shortDate(date, today)

@Composable
fun SectionSpacer() = Spacer(Modifier.height(16.dp))

/** Butoane „−  3  +” pentru numărul de bucăți (minimum 1). */
@Composable
fun PiecesStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(1)) }, enabled = value > 1) { Text("−") }
        Text("$value", modifier = Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(99)) }, enabled = value < 99) { Text("+") }
    }
}
