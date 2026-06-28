package com.mejoresiagratis.lumiai.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode

/** Reducir movimiento efectivo (pref del usuario u opción del sistema). Lo leen orbe/ticks. */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun LumiAiTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accent: AccentColor = AccentColor.YELLOW,
    accentStyle: AccentStyle = AccentStyle.WARM,
    highContrast: Boolean = false,
    reduceMotion: Boolean = false,
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
        // Estilo de paleta por acento:
        // - Blanco -> Monochrome: neutro real (TonalSpot lo teñiría de azul en oscuro).
        // - Ámbar  -> TonalSpot: su semilla ya es clara, el resultado es un oro fiel.
        // - Resto (cromáticos y Multicolor) -> Content: conserva el primary ≈ semilla,
        //   evitando que rojo/violeta/etc. se aclaren a salmón/pastel en tema oscuro.
        // Blanco -> Monochrome (neutro real). El resto sigue el estilo elegido:
        // Cálido -> TonalSpot (oro/tono sobrio), Vívido -> Content (primary ≈ semilla).
        val paletteStyle = when {
            accent == AccentColor.WHITE -> PaletteStyle.Monochrome
            accentStyle == AccentStyle.WARM -> PaletteStyle.TonalSpot
            else -> PaletteStyle.Content
        }
        val gen = rememberDynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            isAmoled = false,
            style = paletteStyle,
            contrastLevel = if (highContrast) 1.0 else 0.0
        )
        // Adoptamos el esquema COMPLETO generado desde la semilla, incluidos los neutros
        // (fondo, superficie, surfaceVariant, contorno y contenedores). Así TODA la vista
        // —bordes, fondos y detalles— refuerza la paleta del acento elegido en lugar de
        // arrastrar los neutros ámbar de marca. Animamos cada rol para que el cambio de
        // modo/acento (incluido Multicolor) sea suave y no haya saltos de color.
        gen.copy(
            primary = animRole(gen.primary, reduceMotion, "primary"),
            onPrimary = animRole(gen.onPrimary, reduceMotion, "onPrimary"),
            primaryContainer = animRole(gen.primaryContainer, reduceMotion, "primaryContainer"),
            onPrimaryContainer = animRole(gen.onPrimaryContainer, reduceMotion, "onPrimaryContainer"),
            secondary = animRole(gen.secondary, reduceMotion, "secondary"),
            onSecondary = animRole(gen.onSecondary, reduceMotion, "onSecondary"),
            secondaryContainer = animRole(gen.secondaryContainer, reduceMotion, "secondaryContainer"),
            onSecondaryContainer = animRole(gen.onSecondaryContainer, reduceMotion, "onSecondaryContainer"),
            tertiary = animRole(gen.tertiary, reduceMotion, "tertiary"),
            onTertiary = animRole(gen.onTertiary, reduceMotion, "onTertiary"),
            tertiaryContainer = animRole(gen.tertiaryContainer, reduceMotion, "tertiaryContainer"),
            onTertiaryContainer = animRole(gen.onTertiaryContainer, reduceMotion, "onTertiaryContainer"),
            background = animRole(gen.background, reduceMotion, "background"),
            onBackground = animRole(gen.onBackground, reduceMotion, "onBackground"),
            surface = animRole(gen.surface, reduceMotion, "surface"),
            onSurface = animRole(gen.onSurface, reduceMotion, "onSurface"),
            surfaceVariant = animRole(gen.surfaceVariant, reduceMotion, "surfaceVariant"),
            onSurfaceVariant = animRole(gen.onSurfaceVariant, reduceMotion, "onSurfaceVariant"),
            surfaceTint = animRole(gen.surfaceTint, reduceMotion, "surfaceTint"),
            outline = animRole(gen.outline, reduceMotion, "outline"),
            outlineVariant = animRole(gen.outlineVariant, reduceMotion, "outlineVariant"),
            surfaceContainerLowest = animRole(gen.surfaceContainerLowest, reduceMotion, "scLowest"),
            surfaceContainerLow = animRole(gen.surfaceContainerLow, reduceMotion, "scLow"),
            surfaceContainer = animRole(gen.surfaceContainer, reduceMotion, "scMid"),
            surfaceContainerHigh = animRole(gen.surfaceContainerHigh, reduceMotion, "scHigh"),
            surfaceContainerHighest = animRole(gen.surfaceContainerHighest, reduceMotion, "scHighest")
        )
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LumiAiTypography,
            shapes = LumiShapes,
            content = content
        )
    }
}

@androidx.compose.runtime.Composable
private fun animRole(target: androidx.compose.ui.graphics.Color, reduce: Boolean, label: String) =
    if (reduce) target else animateColorAsState(target, label = label).value
