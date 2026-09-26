package com.kpop.feature.upscale

import com.kpop.core.model.GalleryItem

data class ProcessedImage(
    val item: GalleryItem,
    val usedExternalService: Boolean,
)

fun interface UpscaleProcessor {
    suspend fun process(item: GalleryItem): Result<ProcessedImage>
}

/**
 * Safe local fallback used until an upscale API or an on-device model is configured.
 * It deliberately does not claim that the original image was AI-upscaled.
 */
class LocalPassThroughProcessor : UpscaleProcessor {
    override suspend fun process(item: GalleryItem): Result<ProcessedImage> =
        Result.success(ProcessedImage(item = item, usedExternalService = false))
}
