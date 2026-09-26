package com.kpop.core.model

data class GalleryItem(
    val id: String,
    val title: String,
    val source: ImageSource,
    val origin: ImageOrigin,
)

sealed interface ImageSource {
    data class Resource(val resourceId: Int) : ImageSource
    data class LocalUri(val value: String) : ImageSource
}

enum class ImageOrigin {
    SAMPLE,
    LOCAL_FILE,
    USB_SCANNER,
}
