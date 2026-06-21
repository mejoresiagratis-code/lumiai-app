package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFFFB300)
private val AmberDark = Color(0xFFC68400)
private val AmberContainerDark = Color(0xFF4A3300)
private val AmberContainerLight = Color(0xFFFFE082)

val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1300),
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = Color(0xFFD7C4A1),
    onSecondary = Color(0xFF221A00),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF22272E),
    onSurfaceVariant = Color(0xFFC2C8D0),
    outline = Color(0xFF8B949E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6E5D3E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDFBF7),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFEDE7DC),
    onSurfaceVariant = Color(0xFF4C4639),
    outline = Color(0xFF7D7767),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)
