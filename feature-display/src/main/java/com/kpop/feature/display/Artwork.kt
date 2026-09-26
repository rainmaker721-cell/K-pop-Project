package com.kpop.feature.display

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import com.kpop.core.model.GalleryItem
import com.kpop.core.model.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
internal fun Artwork(
    item: GalleryItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maximumDecodeDimension: Int = 2_560,
) {
    when (val source = item.source) {
        is ImageSource.Resource -> Image(
            painter = painterResource(source.resourceId),
            contentDescription = item.title,
            contentScale = contentScale,
            modifier = modifier,
        )

        is ImageSource.LocalUri -> LocalUriArtwork(
            source = source.value,
            title = item.title,
            modifier = modifier,
            contentScale = contentScale,
            maximumDecodeDimension = maximumDecodeDimension,
        )
    }
}

@Composable
private fun LocalUriArtwork(
    source: String,
    title: String,
    modifier: Modifier,
    contentScale: ContentScale,
    maximumDecodeDimension: Int,
) {
    val resolver = LocalContext.current.contentResolver
    val loadState by produceState<BitmapLoadState>(BitmapLoadState.Loading, source, maximumDecodeDimension) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeBitmap(resolver, source.toUri(), maximumDecodeDimension) }
                .fold(BitmapLoadState::Success) { BitmapLoadState.Failure }
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF15151A)),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = loadState) {
            BitmapLoadState.Loading -> {
                CircularProgressIndicator(color = Color(0xFFE759FF))
                Text(
                    text = "이미지를 불러오는 중",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            BitmapLoadState.Failure -> Text(
                text = "이미지를 불러올 수 없습니다",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            is BitmapLoadState.Success -> Image(
                bitmap = state.bitmap,
                contentDescription = title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private sealed interface BitmapLoadState {
    data object Loading : BitmapLoadState
    data object Failure : BitmapLoadState
    data class Success(val bitmap: ImageBitmap) : BitmapLoadState
}

private fun decodeBitmap(
    resolver: android.content.ContentResolver,
    uri: Uri,
    maximumDimension: Int,
): ImageBitmap {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val ratio = min(
                maximumDimension.toFloat() / width,
                maximumDimension.toFloat() / height,
            )
            if (ratio < 1f) {
                decoder.setTargetSize((width * ratio).toInt(), (height * ratio).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maximumDimension ||
            bounds.outHeight / sampleSize > maximumDimension
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Unable to decode $uri")
    }
    return bitmap.asImageBitmap()
}
