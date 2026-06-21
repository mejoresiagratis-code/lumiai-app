package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Ejecuta un modo. Es suspend y cancelable: al cancelarse, apaga el LED. */
@Singleton
class FlashEngine @Inject constructor(
    private val torch: TorchController
) {
    suspend fun play(mode: FlashMode, settings: FlashSettings) {
        val s = settings.coerced()
        try {
            when (mode) {
                FlashMode.CONTINUOUS -> { torch.turnOn(s.intensityLevel); awaitCancellation() }
                FlashMode.SCREEN -> { torch.turnOff(); awaitCancellation() }
                FlashMode.STROBE -> strobe(s)
                FlashMode.SOS_MORSE -> morse(s)
            }
        } finally {
            torch.turnOff()
        }
    }

    private suspend fun strobe(s: FlashSettings) {
        val period = (1000f / s.strobeHz).toLong().coerceAtLeast(2L)
        val onMs = period / 2
        val offMs = period - onMs
        while (true) {
            torch.turnOn(s.intensityLevel); delay(onMs)
            torch.turnOff(); delay(offMs)
        }
    }

    private suspend fun morse(s: FlashSettings) {
        val durations = Morse.sos(s.morseUnitMs)
        while (true) {
            for (i in durations.indices) {
                if (i % 2 == 0) torch.turnOn(s.intensityLevel) else torch.turnOff()
                delay(durations[i])
            }
            torch.turnOff()
            delay(s.morseUnitMs * 7)
        }
    }
}
