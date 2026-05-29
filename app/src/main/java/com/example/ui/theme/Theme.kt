package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0F3A2E), // Subtle dark forest green container
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    tertiary = DarkTertiary,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFE2E8F0), // Cool white
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B), // Higher elevation card outlines/fills
    onSurfaceVariant = Color(0xFF94A3B8), // Muted text
    error = DarkTertiary,
    onError = Color.White
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5), // Soft pastel emerald green container
    onPrimaryContainer = Color(0xFF065F46),
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF0F172A), // Slate-900 absolute text
    surface = LightSurface,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9), // Ice blue fields
    onSurfaceVariant = Color(0xFF64748B), // Slate-500 muted text
    error = LightTertiary,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: Int = 0, // 0: Auto/System, 1: Light, 2: Dark
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
