package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.data.*

// Dark theme colors
private val AppDarkBackground = Color(0xFF1A1A1A)
private val AppDarkSurface = Color(0xFF2A2A2A)
private val AppDarkerGray = Color(0xFF0F0F0F)
private val AppFieryOrange = Color(0xFFFF6B35)
private val AppCyanBlue = Color(0xFF00D9FF)
private val AppSoftCyan = Color(0xFF6DD5ED)
private val AppLightText = Color(0xFFE8E8E8)
private val AppMutedText = Color(0xFF999999)
private val AppWhite = Color(0xFFFFFFFF)
private val AppErrorRed = Color(0xFFFF4444)
private val AppOutlineDark = Color(0xFF404040)

// Light theme colors - Clean & Minimal
private val LightPrimary = Color(0xFF6366F1)
private val LightSecondary = Color(0xFFF59E0B)
private val LightTertiary = Color(0xFF10B981)
private val LightBackground = Color(0xFFFAFAFA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF5F5F5)
private val LightOnBackground = Color(0xFF1A1A1A)  // text
private val LightOnSurface = Color(0xFF1A1A1A)     // text
private val LightOutline = Color(0xFFE5E5E5)       // Border color

private val AppDarkColorScheme = darkColorScheme(
    primary = AppCyanBlue,
    onPrimary = AppDarkBackground,
    primaryContainer = AppDarkerGray,
    onPrimaryContainer = AppCyanBlue,
    secondary = AppFieryOrange,
    onSecondary = AppDarkBackground,
    secondaryContainer = AppDarkSurface,
    onSecondaryContainer = AppFieryOrange,
    tertiary = AppSoftCyan,
    onTertiary = AppDarkBackground,
    surface = AppDarkSurface,
    onSurface = AppLightText,
    surfaceVariant = AppDarkerGray,
    onSurfaceVariant = AppMutedText,
    background = AppDarkBackground,
    onBackground = AppLightText,
    error = AppErrorRed,
    outline = AppOutlineDark
)

private val AppLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),

    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),

    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6B6B6B),

    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    outline = LightOutline,
    outlineVariant = Color(0xFFF5F5F5)
)

@Composable
@Preview
fun App() {
    val driverFactory = getPlatformDatabaseFactory()
    val database = remember { AppDatabase(driverFactory.createDriver()) }
    val settingsRepository = remember { SettingsRepository(database) }

    val settings by settingsRepository.getSettings().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        if (settings == null) {
            settingsRepository.updateSettings(
                userName = "",
                notificationsEnabled = true,
                darkModeEnabled = true,
                defaultTaskDuration = 30L
            )
        }
    }

    val isDarkMode = settings?.darkModeEnabled ?: true

    MaterialTheme(
        colorScheme = if (isDarkMode) AppDarkColorScheme else AppLightColorScheme
    ) {
        MainApp(database = database)
    }
}