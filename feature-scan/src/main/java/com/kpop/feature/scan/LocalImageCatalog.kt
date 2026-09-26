package com.kpop.feature.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.kpop.core.model.GalleryItem
import com.kpop.core.model.ImageOrigin
import com.kpop.core.model.ImageSource

class LocalImageCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<GalleryItem> = savedUris().map(::toGalleryItem)

    fun add(uris: List<Uri>): List<GalleryItem> {
        if (uris.isEmpty()) return load()

        uris.forEach { uri ->
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }

        val merged = (savedUris() + uris.map(Uri::toString)).distinct()
        preferences.edit().putString(KEY_URIS, merged.joinToString(SEPARATOR)).apply()
        return merged.map(::toGalleryItem)
    }

    fun remove(item: GalleryItem): List<GalleryItem> {
        val source = item.source as? ImageSource.LocalUri ?: return load()
        val remaining = savedUris().filterNot { it == source.value }
        preferences.edit().putString(KEY_URIS, remaining.joinToString(SEPARATOR)).apply()
        return remaining.map(::toGalleryItem)
    }

    private fun savedUris(): List<String> = preferences
        .getString(KEY_URIS, null)
        ?.split(SEPARATOR)
        ?.filter(String::isNotBlank)
        .orEmpty()

    private fun toGalleryItem(uriValue: String): GalleryItem {
        val uri = Uri.parse(uriValue)
        return GalleryItem(
            id = "local:${uriValue.hashCode()}",
            title = displayName(uri) ?: uri.lastPathSegment ?: "가져온 이미지",
            source = ImageSource.LocalUri(uriValue),
            origin = ImageOrigin.LOCAL_FILE,
        )
    }

    private fun displayName(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }.getOrNull()

    private companion object {
        const val PREFERENCES_NAME = "kpop_local_image_catalog"
        const val KEY_URIS = "persisted_image_uris"
        const val SEPARATOR = "\n"
    }
}

/** Hardware-specific scanner drivers can implement this without leaking USB details into the UI. */
fun interface UsbScannerGateway {
    suspend fun acquireImage(): Result<GalleryItem>
}
