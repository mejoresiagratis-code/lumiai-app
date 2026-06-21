package com.mejoresiagratis.lumiai.util

import com.mejoresiagratis.lumiai.data.torch.TorchController

class FakeTorchController(
    override val hasFlash: Boolean = true,
    override val maxIntensityLevel: Int = 100
) : TorchController {
    var isOn: Boolean = false
        private set
    var lastIntensity: Int = 0
        private set
    val transitions = mutableListOf<Boolean>()

    override fun turnOn(intensityLevel: Int) {
        lastIntensity = intensityLevel
        if (!isOn) transitions.add(true)
        isOn = true
    }

    override fun turnOff() {
        if (isOn) transitions.add(false)
        isOn = false
    }
}
