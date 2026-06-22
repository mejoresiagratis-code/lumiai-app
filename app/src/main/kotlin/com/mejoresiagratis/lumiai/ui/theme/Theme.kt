package com.mejoresiagratis.lumiai.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode

@Composable
fun LumiAiTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accent: AccentColor = AccentColor.AMBER,
    activeMode: FlashMode? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }

    val colorScheme = if (dynamicColor) {
        base
    } else {
        // Esquema M3 armónico generado desde el color de acento (semilla).
        // Multicolor sigue al modo activo. Conservamos los neutros afinados
        // (fondo/superficie/contorno) de la marca y solo adoptamos —animados—
        // los roles de acento generados.
        val seed = resolveAccent(accent, activeMode)
        val gen = rememberDynamicColorScheme(seedColor = seed, isDark = dark)
        val primary by animateColorAsState(gen.primary, label = "primary")
        val onPrimary by animateColorAsState(gen.onPrimary, label = "onPrimary")
        val primaryContainer by animateColorAsState(gen.primaryContainer, label = "primaryContainer")
        val onPrimaryContainer by animateColorAsState(gen.onPrimaryContainer, label = "onPrimaryContainer")
        val secondary by animateColorAsState(gen.secondary, label = "secondary")
        val onSecondary by animateColorAsState(gen.onSecondary, label = "onSecondary")
        val secondaryContainer by animateColorAsState(gen.secondaryContainer, label = "secondaryContainer")
        val onSecondaryContainer by animateColorAsState(gen.onSecondaryContainer, label = "onSecondaryContainer")
        val tertiary by animateColorAsState(gen.tertiary, label = "tertiary")
        val onTertiary by animateColorAsState(gen.onTertiary, label = "onTertiary")
        val tertiaryContainer by animateColorAsState(gen.tertiaryContainer, label = "tertiaryContainer")
        val onTertiaryContainer by animateColorAsState(gen.onTertiaryContainer, label = "onTertiaryContainer")
        base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumiAiTypography,
        shapes = LumiShapes,
        content = content
    )
}
