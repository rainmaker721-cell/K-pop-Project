package com.kpop.display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kpop.core.model.GalleryItem
import com.kpop.core.model.ImageOrigin
import com.kpop.core.model.ImageSource
import com.kpop.display.ui.theme.KPopDisplayTheme
import com.kpop.feature.admin.DisplaySettings
import com.kpop.feature.display.DisplayScreen
import com.kpop.feature.display.PreparingScreen
import com.kpop.feature.display.ScanHomeScreen
import com.kpop.feature.scan.LocalImageCatalog
import com.kpop.feature.upscale.LocalPassThroughProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KPopDisplayTheme {
                KPopApp()
            }
        }
    }
}

@Composable
private fun KPopApp() {
    val context = LocalContext.current
    val catalog = remember(context) { LocalImageCatalog(context) }
    val processor = remember { LocalPassThroughProcessor() }
    val settings = remember { DisplaySettings() }

    val sampleItems = remember {
        listOf(
            GalleryItem(
                id = "sample-01",
                title = "데모 포토카드 01",
                source = ImageSource.Resource(R.drawable.sample_01),
                origin = ImageOrigin.SAMPLE,
            ),
            GalleryItem(
                id = "sample-02",
                title = "데모 포토카드 02",
                source = ImageSource.Resource(R.drawable.sample_02),
                origin = ImageOrigin.SAMPLE,
            ),
            GalleryItem(
                id = "sample-03",
                title = "데모 포토카드 03",
                source = ImageSource.Resource(R.drawable.sample_03),
                origin = ImageOrigin.SAMPLE,
            ),
        )
    }

    var importedItems by remember { mutableStateOf(catalog.load()) }
    val allItems = sampleItems + importedItems

    var screen by rememberSaveable { mutableStateOf(Screen.GALLERY.name) }
    var pendingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var slideshowEnabled by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val scannerTestInput = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val updatedItems = uri?.let { catalog.add(listOf(it)) } ?: catalog.load()
        importedItems = updatedItems
        errorMessage = null
        updatedItems.lastOrNull()?.let { scanned ->
            pendingItemId = scanned.id
            slideshowEnabled = false
            screen = Screen.PREPARING.name
        }
    }

    fun prepare(item: GalleryItem, slideshow: Boolean) {
        errorMessage = null
        pendingItemId = item.id
        slideshowEnabled = slideshow
        screen = Screen.PREPARING.name
    }

    LaunchedEffect(screen, pendingItemId, allItems) {
        if (screen != Screen.PREPARING.name) return@LaunchedEffect
        val item = allItems.firstOrNull { it.id == pendingItemId }
        if (item == null) {
            errorMessage = "선택한 이미지를 찾을 수 없습니다. 다시 선택해 주세요."
            screen = Screen.GALLERY.name
            return@LaunchedEffect
        }

        // Allow the preparing state to render before a future processor begins heavier work.
        yield()
        processor.process(item)
            .onSuccess { processed ->
                selectedItemId = processed.item.id
                screen = Screen.DISPLAY.name
            }
            .onFailure {
                errorMessage = "이미지를 준비하지 못했습니다: ${it.message ?: "알 수 없는 오류"}"
                screen = Screen.GALLERY.name
            }
    }

    LaunchedEffect(screen, slideshowEnabled, selectedItemId, allItems) {
        if (screen != Screen.DISPLAY.name || !slideshowEnabled || allItems.isEmpty()) {
            return@LaunchedEffect
        }

        while (true) {
            delay(settings.slideIntervalMillis)
            val currentIndex = allItems.indexOfFirst { it.id == selectedItemId }
            val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % allItems.size
            selectedItemId = allItems[nextIndex].id
        }
    }

    when (screen) {
        Screen.PREPARING.name -> PreparingScreen()

        Screen.DISPLAY.name -> {
            val selected = allItems.firstOrNull { it.id == selectedItemId }
            if (selected == null) {
                LaunchedEffect(Unit) { screen = Screen.GALLERY.name }
            } else {
                DisplayScreen(
                    item = selected,
                    maximumZoom = settings.maximumZoom,
                    onBackToGallery = {
                        slideshowEnabled = false
                        pendingItemId = null
                        selectedItemId = null
                        screen = Screen.GALLERY.name
                    },
                )
            }
        }

        else -> ScanHomeScreen(
            items = allItems,
            errorMessage = errorMessage,
            onOpenImage = { prepare(it, slideshow = false) },
            onImportImages = { scannerTestInput.launch(arrayOf("image/*")) },
            onStartSlideshow = {
                allItems.firstOrNull()?.let { prepare(it, slideshow = true) }
            },
        )
    }
}

private enum class Screen {
    GALLERY,
    PREPARING,
    DISPLAY,
}
