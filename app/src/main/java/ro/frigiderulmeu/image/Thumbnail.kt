package ro.frigiderulmeu.image

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Cache mic în memorie pentru miniaturi deja decodate (aprox. 1/8 din memoria aplicației). */
private object ThumbCache {
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    val cache = object : LruCache<String, ImageBitmap>(maxKb) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }
}

private fun decodeThumb(path: String, targetPx: Int): ImageBitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
}

/**
 * Afișează fotografia unui produs, decodată la dimensiunea necesară, în afara firului principal.
 * Dacă produsul nu are fotografie, afișează inițialele numelui.
 */
@Composable
fun ProductImage(path: String?, name: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        // Dimensiunea în pixeli a căsuței (ignorăm o latură nelimitată).
        val targetPx = maxOf(
            if (constraints.hasBoundedWidth) constraints.maxWidth else 0,
            if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        ).coerceAtLeast(32)
        if (path == null) {
            Initials(name, Modifier.fillMaxSize())
        } else {
            LoadedImage(path, name, targetPx)
        }
    }
}

@Composable
private fun LoadedImage(path: String, name: String, targetPx: Int) {
    // Grupăm dimensiunile ca să refolosim aceeași miniatură pentru căsuțe apropiate ca mărime.
    val bucket = when {
        targetPx <= 96 -> 96
        targetPx <= 200 -> 200
        else -> 400
    }
    val key = "$path@$bucket"
    var bitmap by remember(key) { mutableStateOf(ThumbCache.cache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                decodeThumb(path, bucket)?.also { ThumbCache.cache.put(key, it) }
            }
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun Initials(name: String, modifier: Modifier) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        val size = (maxWidth.value * 0.38f).coerceIn(8f, 40f)
        Text(
            initials,
            fontSize = size.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1
        )
    }
}
