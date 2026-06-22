package com.mejoresiagratis.lumiai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    // Ámbar conserva el esquema afinado; el resto (incl. Multicolor según el modo
    // activo) sustituye primary/onPrimary. Esquema armónico + animación: Fase 1.3.
    val applyAccent = !dynamicColor && accent != AccentColor.AMBER
    val colorScheme = if (applyAccent) {
        val p = resolveAccent(accent, activeMode)
        base.copy(primary = p, onPrimary = onColorFor(p))
    } else {
        base
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumiAiTypography,
        shapes = LumiShapes,
        content = content
    )
}
