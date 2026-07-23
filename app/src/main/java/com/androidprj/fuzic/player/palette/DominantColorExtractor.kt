package com.androidprj.fuzic.player.palette

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.androidprj.fuzic.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Computes the most-saturated non-grey pixel from a cover-artwork image.
 *
 * The full-player background gradient (UI Track) reads
 * `PlayerUiState.dominantColor`; this extractor is the data-side producer
 * of that field. We deliberately do **not** use `androidx.palette` — a
 * "least-headaches" decision in this increment keeps the dependency
 * graph unchanged.
 *
 * ## Algorithm
 *
 * 1. (Android side) The caller loads a bitmap via the supplied
 *    [ImageLoader]. On success, the bitmap is downsampled to a small
 *    fixed size and its pixels are extracted into an `IntArray` of ARGB
 *    packed ints.
 * 2. (Pure side) [dominantColorOf] walks the pixels and returns the
 *    ARGB int with the highest HSV saturation, ignoring near-grey and
 *    near-black / near-white pixels. This makes the output readable
 *    against the player UI: a dominant-color background needs contrast.
 * 3. On any failure (load error, null URL, no qualified pixel) returns
 *    `null`.
 *
 * ## Why extract at 32×32
 *
 * A 1024-pixel palette scan is enough for a 32-band grid. Anything larger
 * adds CPU per track transition for no visual win at typical cover-art
 * resolutions.
 */
@Singleton
class DominantColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Load [artworkUrl] through [imageLoader] and return its most-saturated
     * pixel, or `null` on any failure / empty input.
     */
    suspend fun extract(
        artworkUrl: String?,
        imageLoader: ImageLoader,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): Int? {
        if (artworkUrl.isNullOrBlank()) return null
        return withContext(ioDispatcher) {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: return@withContext null
            sampleBitmap(bitmap)
        }
    }

    internal fun sampleBitmap(bitmap: Bitmap): Int? {
        val targetSize = 32
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return null
        val scaled = if (width <= targetSize && height <= targetSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, /* filter = */ true)
        }
        val pixels = IntArray(targetSize * targetSize)
        scaled.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
        if (scaled !== bitmap) scaled.recycle()
        return dominantColorOf(pixels)
    }
}

/**
 * Pure (Android-free) palette scanner.
 *
 * Top-level function so JVM unit tests can call it without instantiating
 * the surrounding [DominantColorExtractor] class (which needs a real
 * Android `Context`). Walks every ARGB int in [pixels] and returns the
 * one with the highest HSV saturation, ignoring near-grey, near-black,
 * near-white, and near-transparent pixels.
 *
 * ARGB packing: `(0xAARRGGBB).toInt()` — alpha is the top byte, blue is
 * the bottom. We bit-shift directly so the function is JVM-testable
 * without stubbing `android.graphics.Color`.
 */
fun dominantColorOf(pixels: IntArray): Int? {
    if (pixels.isEmpty()) return null
    var bestArgb: Int = 0
    var bestSaturation: Float = -1f
    for (argb in pixels) {
        val a = (argb ushr 24) and 0xFF
        if (a < 32) continue // near-transparent — drop
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        val maxChannel = max(r, max(g, b))
        val minChannel = min(r, min(g, b))
        val value = maxChannel / 255f
        if (value < 0.1f || value > 0.95f) continue // near-black / near-white
        val saturation =
            if (maxChannel == 0) 0f else (maxChannel - minChannel).toFloat() / maxChannel
        if (saturation < 0.15f) continue // near-grey — drop
        if (saturation > bestSaturation) {
            bestSaturation = saturation
            bestArgb = argb
        }
    }
    return if (bestSaturation >= 0f) bestArgb else null
}

/** Pure helpers used by tests to build ARGB literals without touching `android.graphics.Color`. */
internal fun rgbArgb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or (r shl 16) or (g shl 8) or b

internal fun argbArgb(a: Int, r: Int, g: Int, b: Int): Int =
    (a shl 24) or (r shl 16) or (g shl 8) or b

internal const val BLACK_ARGB: Int = (0xFF shl 24) or 0
internal const val WHITE_ARGB: Int = -1 // 0xFFFFFFFF
