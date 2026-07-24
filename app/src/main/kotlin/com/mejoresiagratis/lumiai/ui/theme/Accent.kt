package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.FlashMode

// Valores sólidos (de la lista del mockup).
private val OrangeAccent = Color(0xFFFF7A1A)
private val AmberAccent = Color(0xFFFFB300)
private val YellowAccent = Color(0xFFFFD60A)
private val WhiteAccent = Color(0xFFF2F4F8)
private val RedAccent = Color(0xFFE12B2B)
private val BlueAccent = Color(0xFF4D7BFF)
private val GreenAccent = Color(0xFF34C759)
private val VioletAccent = Color(0xFF9B6CFF)
private val TealAccent = Color(0xFF11A693)
private val MagentaAccent = Color(0xFFE84393)

/** Paleta por modo para el acento Multicolor (la consumirá el Hub en la Fase 2). */
fun modeAccentColor(mode: FlashMode): Color = when (mode) {
    FlashMode.CONTINUOUS -> AmberAccent
    FlashMode.SCREEN -> BlueAccent
    FlashMode.SOS_MORSE -> RedAccent
    FlashMode.STROBE -> VioletAccent
    FlashMode.TEXT_MORSE -> TealAccent
    FlashMode.BEACON -> GreenAccent
    FlashMode.MUSIC -> MagentaAccent
}

/** Color sólido de un acento. Multicolor cae al azul de marca a nivel global (su efecto por modo vive en el Hub). */
fun AccentColor.solidColor(): Color = when (this) {
    AccentColor.MULTICOLOR -> BlueAccent
    AccentColor.BLUE -> BlueAccent
    AccentColor.ORANGE -> OrangeAccent
    AccentColor.AMBER -> AmberAccent
    AccentColor.YELLOW -> YellowAccent
    AccentColor.GREEN -> GreenAccent
    AccentColor.RED -> RedAccent
    AccentColor.VIOLET -> VioletAccent
    AccentColor.WHITE -> WhiteAccent
}

/** Resuelve el acento efectivo: Multicolor + modo conocido -> color del modo; si no, color sólido. */
fun resolveAccent(accent: AccentColor, mode: FlashMode? = null): Color =
    if (accent == AccentColor.MULTICOLOR && mode != null) modeAccentColor(mode) else accent.solidColor()

/** Color de texto/icono legible sobre un fondo de acento (negro o blanco según luminancia). */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF1A1300) else Color(0xFFFFFFFF)
