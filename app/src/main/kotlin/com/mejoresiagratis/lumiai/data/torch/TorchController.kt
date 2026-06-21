package com.mejoresiagratis.lumiai.data.torch

/** Unica abstraccion que controla el LED por hardware. */
interface TorchController {
    val hasFlash: Boolean
    val maxIntensityLevel: Int
    fun turnOn(intensityLevel: Int)
    fun turnOff()
}
