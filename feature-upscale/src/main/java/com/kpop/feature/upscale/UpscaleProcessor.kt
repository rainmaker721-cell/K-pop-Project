package com.kpop.feature.upscale

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.kpop.core.model.GalleryItem
import com.kpop.core.model.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

data class ProcessedImage(
    val item: GalleryItem,
    val outputWidth: Int,
    val outputHeight: Int,
    val method: UpscaleMethod,
)

enum class UpscaleMethod {
    LOCAL_HIGH_QUALITY_4K,
}

data class PixelSize(val width: Int, val height: Int)

object FourKTarget {
    const val FOUR_K_LONG_EDGE = 3_840

    /** Makes the image's long edge 3840 pixels while preserving its original aspect ratio. */
    fun sizeFor(sourceWidth: Int, sourceHeight: Int): PixelSize {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be positive" }

        val landscape = sourceWidth >= sourceHeight
        val scale = if (landscape) {
            FOUR_K_LONG_EDGE.toDouble() / sourceWidth
        } else {
            FOUR_K_LONG_EDGE.toDouble() / sourceHeight
        }

        return PixelSize(
            width = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
            height = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
        )
    }
}

fun interface UpscaleProcessor {
    suspend fun process(item: GalleryItem): Result<ProcessedImage>
}

/**
 * Offline 4K processor for the Android display product.
 *
 * This performs multi-step filtered resampling. It creates a real UHD-sized output but does not
 * claim to invent AI detail. Results are cached in private app storage and reused for later display.
 */
class Android4KUpscaleProcessor(context: Context) : UpscaleProcessor {
    private val appContext = context.applicationContext
    private val outputDirectory = File(appContext.filesDir, "upscaled-4k")

    override suspend fun process(item: GalleryItem): Result<ProcessedImage> =
        withContext(Dispatchers.Default) {
            runCatching { upscale(item) }
        }

    private fun upscale(item: GalleryItem): ProcessedImage {
        check(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Unable to create 4K output directory"
        }

        val outputFile = File(outputDirectory, cacheFileName(item))
        cachedSize(outputFile)?.let { size ->
            return processedResult(item, outputFile, size)
        }

        val original = decodeSource(item.source)
        val targetSize = FourKTarget.sizeFor(original.width, original.height)
        val upscaled = resizeInQualitySteps(original, targetSize)
        val temporaryFile = File(outputDirectory, "${outputFile.name}.tmp")

        try {
            FileOutputStream(temporaryFile).use { output ->
                check(upscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Unable to encode 4K image"
                }
            }
            if (outputFile.exists()) outputFile.delete()
            check(temporaryFile.renameTo(outputFile)) { "Unable to store 4K image" }
        } finally {
            temporaryFile.delete()
            if (upscaled !== original) upscaled.recycle()
            original.recycle()
        }

        return processedResult(item, outputFile, targetSize)
    }

    private fun processedResult(
        originalItem: GalleryItem,
        outputFile: File,
        size: PixelSize,
    ): ProcessedImage = ProcessedImage(
        item = originalItem.copy(source = ImageSource.LocalUri(Uri.fromFile(outputFile).toString())),
        outputWidth = size.width,
        outputHeight = size.height,
        method = UpscaleMethod.LOCAL_HIGH_QUALITY_4K,
    )

    private fun cacheFileName(item: GalleryItem): String {
        val cacheKey = "${item.id}:${item.source}:uhd-v2-long-edge"
        return "${cacheKey.hashCode().toUInt().toString(16)}.jpg"
    }

    private fun cachedSize(file: File): PixelSize? {
        if (!file.isFile || file.length() == 0L) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            PixelSize(options.outWidth, options.outHeight)
        } else {
            file.delete()
            null
        }
    }

    private fun decodeSource(source: ImageSource): Bitmap = when (source) {
        is ImageSource.Resource -> decodeDrawable(source.resourceId)
        is ImageSource.LocalUri -> decodeUri(Uri.parse(source.value))
    }

    private fun decodeDrawable(resourceId: Int): Bitmap {
        val drawable = checkNotNull(appContext.getDrawable(resourceId)) {
            "Unable to load image resource $resourceId"
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }

    private fun decodeUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(appContext.contentResolver, uri)) {
                    decoder,
                    info,
                    _,
                ->
                val ratio = min(
                    MAXIMUM_INPUT_DIMENSION.toFloat() / info.size.width,
                    MAXIMUM_INPUT_DIMENSION.toFloat() / info.size.height,
                )
                if (ratio < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * ratio).roundToInt(),
                        (info.size.height * ratio).roundToInt(),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val resolver = appContext.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to inspect source image" }

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAXIMUM_INPUT_DIMENSION ||
                bounds.outHeight / sampleSize > MAXIMUM_INPUT_DIMENSION
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Unable to decode source image")
        }
    }

    private fun resizeInQualitySteps(source: Bitmap, target: PixelSize): Bitmap {
        if (source.width == target.width && source.height == target.height) return source

        // A large downscale is best handled in one filtered pass. Upscaling uses successive passes
        // to avoid the rougher result produced by one very large interpolation jump.
        if (target.width < source.width || target.height < source.height) {
            return Bitmap.createScaledBitmap(source, target.width, target.height, true)
        }

        var current = source
        while (current.width * 2 < target.width && current.height * 2 < target.height) {
            val next = Bitmap.createScaledBitmap(
                current,
                (current.width * 2).coerceAtMost(target.width),
                (current.height * 2).coerceAtMost(target.height),
                true,
            )
            if (current !== source) current.recycle()
            current = next
        }

        if (current.width != target.width || current.height != target.height) {
            val finalBitmap = Bitmap.createScaledBitmap(current, target.width, target.height, true)
            if (current !== source) current.recycle()
            current = finalBitmap
        }
        return current
    }

    private companion object {
        const val JPEG_QUALITY = 95
        const val MAXIMUM_INPUT_DIMENSION = 4_096
    }
}
