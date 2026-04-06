package com.example.bloqueo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StayFocusedColorScheme = darkColorScheme(
    primary = AccentTeal,
    secondary = PurpleAccent,
    tertiary = RedStrict,
    background = BackgroundDark,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun BloqueoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StayFocusedColorScheme,
        typography = Typography,
        content = content
    )
}