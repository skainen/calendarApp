package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview

// Custom minimal color scheme
private val LightBlueGray = Color(0xFFF5F7FA)  // Very light blue-gray background
private val LightBlue = Color(0xFFE3F2FD)      // Light blue for text bubbles
private val MediumBlue = Color(0xFF2196F3)     // Medium blue for accents
private val DarkText = Color(0xFF1A1A1A)       // Dark text
private val GrayText = Color(0xFF666666)       // Gray text

private val MinimalColorScheme = lightColorScheme(
    primary = LightBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,  // Remove purple backgrounds
    onPrimaryContainer = DarkText,
    secondary = MediumBlue,
    onSecondary = LightBlue,
    secondaryContainer = LightBlue,  // Light blue for task cards
    onSecondaryContainer = DarkText,
    surface = LightBlue,
    onSurface = DarkText,
    surfaceVariant = LightBlue,      // Light blue for chat bubbles
    onSurfaceVariant = DarkText,
    background = LightBlue,      // Very light blue-gray
    onBackground = DarkText,
    error = Color(0xFFE53935),
    errorContainer = Color(0xFFFFEBEE),
    outline = Color(0xFFE0E0E0)
)

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = MinimalColorScheme
    ) {
        MainApp()
    }
}