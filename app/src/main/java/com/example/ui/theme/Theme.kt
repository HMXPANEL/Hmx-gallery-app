package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HmxColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedContainer,
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    background = SophisticatedBg,
    onBackground = SophisticatedTextPrimary,
    surface = SophisticatedSurface,
    onSurface = SophisticatedTextPrimary,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedTextSecondary,
    outline = SophisticatedBorder,
    error = SophisticatedDestructive,
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Sophisticated Dark design theme
    MaterialTheme(
        colorScheme = HmxColorScheme,
        typography = Typography,
        content = content
    )
}
