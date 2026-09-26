package com.kpop.feature.display

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kpop.core.display.ZoomMath
import com.kpop.core.model.GalleryItem

@Composable
fun DisplayScreen(
    item: GalleryItem,
    onBackToGallery: () -> Unit,
    modifier: Modifier = Modifier,
    maximumZoom: Float = 5f,
) {
    ImmersiveDisplayEffect()

    // The exhibition requirement is explicit: only the on-screen control exits.
    BackHandler(enabled = true) { /* Intentionally consume system back. */ }

    var scale by rememberSaveable(item.id) { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable(item.id) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(item.id) { mutableFloatStateOf(0f) }
    var viewportWidth by remember(item.id) { mutableFloatStateOf(0f) }
    var viewportHeight by remember(item.id) { mutableFloatStateOf(0f) }

    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Artwork(
            item = item,
            contentScale = ContentScale.Fit,
            maximumDecodeDimension = 4_096,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    viewportWidth = it.width.toFloat()
                    viewportHeight = it.height.toFloat()
                    offsetX = ZoomMath.clampTranslation(offsetX, viewportWidth, scale)
                    offsetY = ZoomMath.clampTranslation(offsetY, viewportHeight, scale)
                }
                .pointerInput(item.id, maximumZoom) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = ZoomMath.clampScale(
                            value = scale * zoom,
                            maximum = maximumZoom,
                        )
                        scale = nextScale
                        offsetX = ZoomMath.clampTranslation(
                            value = if (nextScale > 1f) offsetX + pan.x else 0f,
                            viewportSizePx = viewportWidth,
                            scale = nextScale,
                        )
                        offsetY = ZoomMath.clampTranslation(
                            value = if (nextScale > 1f) offsetY + pan.y else 0f,
                            viewportSizePx = viewportHeight,
                            scale = nextScale,
                        )
                    }
                }
                .pointerInput(item.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) resetTransform() else scale = 2f.coerceAtMost(maximumZoom)
                        },
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .semantics {
                    contentDescription = "목록으로 돌아가기"
                    role = Role.Button
                }
                .clickable {
                    resetTransform()
                    onBackToGallery()
                },
        ) {
            Canvas(Modifier.size(20.dp)) {
                val color = Color.White
                val stroke = 2.4.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.78f, size.height * 0.5f),
                    end = Offset(size.width * 0.24f, size.height * 0.5f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.24f, size.height * 0.5f),
                    end = Offset(size.width * 0.48f, size.height * 0.26f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.24f, size.height * 0.5f),
                    end = Offset(size.width * 0.48f, size.height * 0.74f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun ImmersiveDisplayEffect() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()

    DisposableEffect(view, activity) {
        val window = activity?.window
        if (window != null && !view.isInEditMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null && !view.isInEditMode) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
