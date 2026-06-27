package com.mejoresiagratis.lumiai.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
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
        // Estilo de paleta por acento:
        // - Blanco -> Monochrome: neutro real (TonalSpot lo teñiría de azul en oscuro).
        // - Ámbar  -> TonalSpot: su semilla ya es clara, el resultado es un oro fiel.
        // - Resto (cromáticos y Multicolor) -> Content: conserva el primary ≈ semilla,
        //   evitando que rojo/violeta/etc. se aclaren a salmón/pastel en tema oscuro.
        val paletteStyle = when (accent) {
            AccentColor.WHITE -> PaletteStyle.Monochrome
            AccentColor.AMBER -> PaletteStyle.TonalSpot
            else -> PaletteStyle.Content
        }
        val gen = rememberDynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            isAmoled = false,
            style = paletteStyle
        )
        // Adoptamos el esquema COMPLETO generado desde la semilla, incluidos los neutros
        // (fondo, superficie, surfaceVariant, contorno y contenedores). Así TODA la vista
        // —bordes, fondos y detalles— refuerza la paleta del acento elegido en lugar de
        // arrastrar los neutros ámbar de marca. Animamos cada rol para que el cambio de
        // modo/acento (incluido Multicolor) sea suave y no haya saltos de color.
        gen.copy(
            primary = animateColorAsState(gen.primary, label = "primary").value,
            onPrimary = animateColorAsState(gen.onPrimary, label = "onPrimary").value,
            primaryContainer = animateColorAsState(gen.primaryContainer, label = "primaryContainer").value,
            onPrimaryContainer = animateColorAsState(gen.onPrimaryContainer, label = "onPrimaryContainer").value,
            secondary = animateColorAsState(gen.secondary, label = "secondary").value,
            onSecondary = animateColorAsState(gen.onSecondary, label = "onSecondary").value,
            secondaryContainer = animateColorAsState(gen.secondaryContainer, label = "secondaryContainer").value,
            onSecondaryContainer = animateColorAsState(gen.onSecondaryContainer, label = "onSecondaryContainer").value,
            tertiary = animateColorAsState(gen.tertiary, label = "tertiary").value,
            onTertiary = animateColorAsState(gen.onTertiary, label = "onTertiary").value,
            tertiaryContainer = animateColorAsState(gen.tertiaryContainer, label = "tertiaryContainer").value,
            onTertiaryContainer = animateColorAsState(gen.onTertiaryContainer, label = "onTertiaryContainer").value,
            background = animateColorAsState(gen.background, label = "background").value,
            onBackground = animateColorAsState(gen.onBackground, label = "onBackground").value,
            surface = animateColorAsState(gen.surface, label = "surface").value,
            onSurface = animateColorAsState(gen.onSurface, label = "onSurface").value,
            surfaceVariant = animateColorAsState(gen.surfaceVariant, label = "surfaceVariant").value,
            onSurfaceVariant = animateColorAsState(gen.onSurfaceVariant, label = "onSurfaceVariant").value,
            surfaceTint = animateColorAsState(gen.surfaceTint, label = "surfaceTint").value,
            outline = animateColorAsState(gen.outline, label = "outline").value,
            outlineVariant = animateColorAsState(gen.outlineVariant, label = "outlineVariant").value,
            surfaceContainerLowest = animateColorAsState(gen.surfaceContainerLowest, label = "scLowest").value,
            surfaceContainerLow = animateColorAsState(gen.surfaceContainerLow, label = "scLow").value,
            surfaceContainer = animateColorAsState(gen.surfaceContainer, label = "scMid").value,
            surfaceContainerHigh = animateColorAsState(gen.surfaceContainerHigh, label = "scHigh").value,
            surfaceContainerHighest = animateColorAsState(gen.surfaceContainerHighest, label = "scHighest").value
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumiAiTypography,
        shapes = LumiShapes,
        content = content
    )
}
