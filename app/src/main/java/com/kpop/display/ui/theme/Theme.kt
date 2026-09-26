package com.kpop.display.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KPopColors = darkColorScheme(
    primary = Color(0xFFE759FF),
    onPrimary = Color(0xFF1C001F),
    secondary = Color(0xFF65E9FF),
    background = Color(0xFF08080C),
    onBackground = Color(0xFFF7F1FA),
    surface = Color(0xFF17131D),
    onSurface = Color(0xFFF7F1FA),
    error = Color(0xFFFF6B81),
)

@Composable
fun KPopDisplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KPopColors,
        content = content,
    )
}
