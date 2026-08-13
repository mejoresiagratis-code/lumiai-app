package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta un modo reaccionando en vivo a los ajustes.
 * - on/off y cambio de modo se gestionan fuera (cancelando/relanzando play).
 * - Los cambios de AJUSTE llegan por el Flow y se aplican SIN reiniciar el modo:
 *   en Continuo solo se actualiza el nivel (suave en API 33+, sin apagar/encender).
 */
@Singleton
class FlashEngine @Inject constructor(
    private val torch: TorchController
) {
    suspend fun play(mode: FlashMode, settings: Flow<FlashSettings>) {
        try {
            when (mode) {
                FlashMode.CONTINUOUS -> settings.collect { torch.turnOn(it.coerced().intensityLevel) }
                FlashMode.SCREEN -> { torch.turnOff(); awaitCancellation() }
                FlashMode.STROBE -> settings.collectLatest { strobe(it.coerced()) }
                FlashMode.SOS_MORSE -> settings.collectLatest { morse(it.coerced()) }
                FlashMode.TEXT_MORSE -> settings.collectLatest { textMorse(it.coerced()) }
                FlashMode.BEACON -> settings.collectLatest { beacon(it.coerced()) }
                // Musica no usa este motor (MusicFlashService controla el LED); defensivo.
                FlashMode.MUSIC -> { torch.turnOff(); awaitCancellation() }
            }
        } finally {
            torch.turnOff()
        }
    }

    // Los huecos DENTRO de un patron activo usan pulseOff() (experimental, QA 13-ago):
    // en dispositivos compatibles no apaga de verdad, evita el parpadeo del indicador
    // del sistema en cada pulso. El apagado REAL solo ocurre al terminar la sesion
    // entera (el `finally` de play(), y los casos defensivos de mensaje vacio/SCREEN/
    // MUSIC mas abajo) — nunca a mitad de un patron en marcha.

    private suspend fun beacon(s: FlashSettings) {
        val onMs = s.beaconFlashMs.coerceAtLeast(1L)
        val offMs = (s.beaconIntervalMs - s.beaconFlashMs).coerceAtLeast(1L)
        while (true) {
            torch.turnOn(s.intensityLevel); delay(onMs)
            torch.pulseOff(); delay(offMs)
        }
    }

    private suspend fun strobe(s: FlashSettings) {
        val period = (1000f / s.strobeHz).toLong().coerceAtLeast(2L)
        val onMs = period / 2
        val offMs = period - onMs
        while (true) {
            torch.turnOn(s.intensityLevel); delay(onMs)
            torch.pulseOff(); delay(offMs)
        }
    }

    private suspend fun textMorse(s: FlashSettings) {
        val durations = Morse.toDurations(s.morseText, s.morseUnitMs)
        if (durations.isEmpty()) { torch.turnOff(); return } // nada que transmitir: apagado real
        while (true) {
            for (i in durations.indices) {
                if (i % 2 == 0) torch.turnOn(s.intensityLevel) else torch.pulseOff()
                delay(durations[i])
            }
            torch.pulseOff() // hueco entre repeticiones del mensaje: sigue "transmitiendo"
            delay(s.morseUnitMs * 7)
        }
    }

    private suspend fun morse(s: FlashSettings) {
        val durations = Morse.sos(s.morseUnitMs)
        while (true) {
            for (i in durations.indices) {
                if (i % 2 == 0) torch.turnOn(s.intensityLevel) else torch.pulseOff()
                delay(durations[i])
            }
            torch.pulseOff() // hueco entre repeticiones del SOS: sigue "transmitiendo"
            delay(s.morseUnitMs * 7)
        }
    }
}
