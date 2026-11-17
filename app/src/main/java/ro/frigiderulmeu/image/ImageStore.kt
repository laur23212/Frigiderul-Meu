package ro.frigiderulmeu.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Transformă o fotografie (de la cameră sau din galerie) într-un fișier WebP mic (max. 400 px),
 * fără metadate EXIF, salvat în spațiul privat al aplicației. Originalul nu este păstrat.
 */
class ImageStore(private val context: Context) {

    private val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }
    private val pendingDir: File get() = File(context.cacheDir, "pending").apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    /** Fișierul temporar în care aplicația de cameră scrie fotografia originală. */
    fun newCameraFile(): File = File(cameraDir, "capture.jpg").apply { if (exists()) delete() }

    /** Procesează imaginea și o pune într-un fișier temporar; devine permanentă abia la salvare. */
    suspend fun processToPending(source: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(source, MAX_SIDE) ?: return@runCatching null
            val out = File(pendingDir, "${UUID.randomUUID()}.webp")
            FileOutputStream(out).use { stream ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY
                else Bitmap.CompressFormat.WEBP
                bitmap.compress(format, QUALITY, stream)
            }
            bitmap.recycle()
            out
        }.getOrNull().also {
            // Originalul de la cameră (câțiva MB) se șterge imediat.
            File(cameraDir, "capture.jpg").delete()
        }
    }

    /** Fotografia redusă la max. 1200 px, pentru ecranul de încadrare în pătrat. */
    suspend fun loadForCrop(source: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { decodeScaled(source, CROP_SOURCE_SIDE) }.getOrNull().also {
            File(cameraDir, "capture.jpg").delete()
        }
    }

    /** Decupează pătratul ales din [source], îl reduce la max. 400 px și îl salvează ca WebP temporar. */
    suspend fun saveSquare(source: Bitmap, left: Int, top: Int, size: Int): File? = withContext(Dispatchers.IO) {
        runCatching {
            val side = size.coerceIn(1, minOf(source.width, source.height))
            val x = left.coerceIn(0, source.width - side)
            val y = top.coerceIn(0, source.height - side)
            val square = Bitmap.createBitmap(source, x, y, side, side)
            val finalSide = minOf(side, MAX_SIDE)
            val scaled = if (finalSide < side) Bitmap.createScaledBitmap(square, finalSide, finalSide, true) else square
            val out = File(pendingDir, "${UUID.randomUUID()}.webp")
            FileOutputStream(out).use { stream ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY
                else Bitmap.CompressFormat.WEBP
                scaled.compress(format, QUALITY, stream)
            }
            out
        }.getOrNull()
    }

    /** Mută o imagine temporară în dosarul permanent și întoarce calea finală. */
    fun commit(path: String?): String? {
        if (path == null) return null
        val file = File(path)
        if (!file.exists()) return null
        if (file.parentFile == photosDir) return path
        val target = File(photosDir, file.name)
        return if (file.renameTo(target)) target.absolutePath else {
            file.copyTo(target, overwrite = true); file.delete(); target.absolutePath
        }
    }

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null

        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        if (matrix.isIdentity) return decoded
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }

    companion object {
        const val MAX_SIDE = 400
        const val QUALITY = 78
        const val CROP_SOURCE_SIDE = 1200

        @Volatile private var instance: ImageStore? = null
        fun get(context: Context): ImageStore =
            instance ?: synchronized(this) {
                instance ?: ImageStore(context.applicationContext).also { instance = it }
            }

        /** Dimensiunea finală, utilă pentru teste. */
        fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
            val longSide = max(width, height)
            if (longSide <= maxSide) return width to height
            val s = maxSide.toFloat() / longSide
            return (width * s).roundToInt() to (height * s).roundToInt()
        }
    }
}
