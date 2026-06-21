package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode

/** Controles de ajuste que un modo puede exponer. Anadir aqui al crecer la app. */
enum class ModeControl { INTENSITY, STROBE_HZ, MORSE_SPEED }

/** Los modos de LED requieren flash; el modo Pantalla no. */
fun FlashMode.requiresFlash(): Boolean = this != FlashMode.SCREEN

/** Que controles pide cada modo. */
fun FlashMode.controls(): Set<ModeControl> = when (this) {
    FlashMode.CONTINUOUS -> setOf(ModeControl.INTENSITY)
    FlashMode.STROBE -> setOf(ModeControl.INTENSITY, ModeControl.STROBE_HZ)
    FlashMode.SOS_MORSE -> setOf(ModeControl.INTENSITY, ModeControl.MORSE_SPEED)
    FlashMode.TEXT_MORSE -> setOf(ModeControl.INTENSITY, ModeControl.MORSE_SPEED)
    FlashMode.SCREEN -> emptySet()
}

/** Si la capacidad que exige un control esta disponible en el dispositivo. */
fun ModeControl.isAvailable(caps: DeviceCapabilities): Boolean = when (this) {
    ModeControl.INTENSITY -> caps.supportsTorchStrength
    ModeControl.STROBE_HZ, ModeControl.MORSE_SPEED -> true
}

/** Si el modo es usable en este dispositivo. */
fun FlashMode.isAvailable(caps: DeviceCapabilities): Boolean =
    !requiresFlash() || caps.hasFlash
