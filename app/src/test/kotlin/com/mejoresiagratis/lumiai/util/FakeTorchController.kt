package com.mejoresiagratis.lumiai.util

import com.mejoresiagratis.lumiai.data.torch.TorchController
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeTorchController(
    override val hasFlash: Boolean = true,
    override val maxIntensityLevel: Int = 100
) : TorchController {
    // Expuesto como MutableSharedFlow (no solo Flow) para que los tests puedan simular
    // un apagado externo con `torch.externalOffEvents.tryEmit(Unit)`.
    val externalOffEventsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val externalOffEvents = externalOffEventsFlow
    var isOn: Boolean = false
        private set
    var lastIntensity: Int = 0
        private set
    val transitions = mutableListOf<Boolean>()
    var pulseOffCalls: Int = 0
        private set

    override fun turnOn(intensityLevel: Int) {
        lastIntensity = intensityLevel
        if (!isOn) transitions.add(true)
        isOn = true
    }

    override fun turnOff() {
        if (isOn) transitions.add(false)
        isOn = false
    }

    override fun pulseOff() {
        pulseOffCalls++
        // Espeja Camera2TorchController: con intensidad variable, se queda ENCENDIDA
        // al minimo (isOn NO cambia); sin ella, cae a un apagado real identico a turnOff().
        if (maxIntensityLevel > 1) {
            lastIntensity = 1
        } else {
            turnOff()
        }
    }
}
