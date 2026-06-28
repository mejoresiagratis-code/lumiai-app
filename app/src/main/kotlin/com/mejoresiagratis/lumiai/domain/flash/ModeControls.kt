package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode

/** Controles de ajuste que un modo puede exponer. Anadir aqui al crecer la app. */
enum class ModeControl { INTENSITY, STROBE_HZ, MORSE_SPEED, BEACON_INTERVAL, BEACON_FLASH }

/** Los modos de LED requieren flash; Pantalla y Baliza no (Baliza cae a la pantalla). */
fun FlashMode.requiresFlash(): Boolean = this != FlashMode.SCREEN && this != FlashMode.BEACON

/** Modos que necesitan microfono (IA de audio). Hoy ninguno; se activara con Alerta Sonora. */
fun FlashMode.requiresMicrophone(): Boolean = false

/** Que controles pide cada modo. */
fun FlashMode.controls(): Set<ModeControl> = when (this) {
    FlashMode.CONTINUOUS -> setOf(ModeControl.INTENSITY)
    FlashMode.STROBE -> setOf(ModeControl.INTENSITY, ModeControl.STROBE_HZ)
    FlashMode.SOS_MORSE -> setOf(ModeControl.INTENSITY, ModeControl.MORSE_SPEED)
    FlashMode.TEXT_MORSE -> setOf(ModeControl.INTENSITY, ModeControl.MORSE_SPEED)
    FlashMode.BEACON -> setOf(ModeControl.INTENSITY, ModeControl.BEACON_INTERVAL, ModeControl.BEACON_FLASH)
    FlashMode.SCREEN -> emptySet()
}

/** Si la capacidad que exige un control esta disponible en el dispositivo. */
fun ModeControl.isAvailable(caps: DeviceCapabilities): Boolean = when (this) {
    ModeControl.INTENSITY -> caps.supportsTorchStrength
    ModeControl.STROBE_HZ, ModeControl.MORSE_SPEED -> true
    ModeControl.BEACON_INTERVAL, ModeControl.BEACON_FLASH -> true
}

/**
 * Regla pura de disponibilidad de un modo segun lo que exige y lo que ofrece el dispositivo.
 * Aislada del enum para poder probarla con todas las combinaciones.
 */
fun modeAvailable(
    requiresFlash: Boolean,
    requiresMicrophone: Boolean,
    caps: DeviceCapabilities
): Boolean =
    (!requiresFlash || caps.hasFlash) && (!requiresMicrophone || caps.hasMicrophone)

/** Si el modo es usable en este dispositivo. */
fun FlashMode.isAvailable(caps: DeviceCapabilities): Boolean =
    modeAvailable(requiresFlash(), requiresMicrophone(), caps)
