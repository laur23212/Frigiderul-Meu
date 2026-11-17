package ro.frigiderulmeu.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_ZOOM = 5f

/**
 * Încadrarea pozei în pătratul din aplicație: se vede exact ce va apărea în calendar și în inventar.
 * Poza se mută cu degetul și se mărește prin ciupire; pătratul este mereu complet acoperit de poză.
 * [onDone] primește colțul stâng-sus și latura pătratului, în pixeli ai pozei originale ([source]).
 */
@Composable
fun CropDialog(source: Bitmap, onCancel: () -> Unit, onDone: (left: Int, top: Int, size: Int) -> Unit) {
    val image = remember(source) { source.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableFloatStateOf(0f) } // latura pătratului, în pixeli

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Încadrează poza", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Așa va apărea în aplicație. Trage poza ca s-o muți, ciupește ca s-o mărești sau s-o micșorezi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    val side = constraints.maxWidth.toFloat()
                    viewport = side
                    val coverScale = side / min(source.width, source.height)
                    val shownW = source.width * coverScale
                    val shownH = source.height * coverScale

                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop, // la zoom 1 poza acoperă exact pătratul
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(side) {
                                detectTransformGestures { _, pan, zoomChange, _ ->
                                    zoom = (zoom * zoomChange).coerceIn(1f, MAX_ZOOM)
                                    val maxX = ((shownW * zoom - side) / 2f).coerceAtLeast(0f)
                                    val maxY = ((shownH * zoom - side) / 2f).coerceAtLeast(0f)
                                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                }
                            }
                            .graphicsLayer {
                                scaleX = zoom; scaleY = zoom
                                translationX = offsetX; translationY = offsetY
                            }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Renunță") }
                    Button(
                        onClick = {
                            val rect = cropRect(source.width, source.height, viewport, zoom, offsetX, offsetY)
                            onDone(rect.first, rect.second, rect.third)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Folosește poza") }
                }
            }
        }
    }
}

/**
 * Pătratul vizibil, în pixeli ai pozei: (stânga, sus, latură). Funcție pură, ca să poată fi testată.
 * [viewport] este latura pătratului de pe ecran; [offsetX]/[offsetY] sunt deplasările pozei, în pixeli de ecran.
 */
fun cropRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewport: Float,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
): Triple<Int, Int, Int> {
    val shortSide = min(bitmapWidth, bitmapHeight)
    if (viewport <= 0f) return Triple((bitmapWidth - shortSide) / 2, (bitmapHeight - shortSide) / 2, shortSide)
    val scale = viewport / shortSide * zoom // pixeli de ecran pe pixel de poză
    val side = (viewport / scale).roundToInt().coerceIn(1, shortSide)
    val centerX = bitmapWidth / 2f - offsetX / scale
    val centerY = bitmapHeight / 2f - offsetY / scale
    val left = (centerX - side / 2f).roundToInt().coerceIn(0, bitmapWidth - side)
    val top = (centerY - side / 2f).roundToInt().coerceIn(0, bitmapHeight - side)
    return Triple(left, top, side)
}
