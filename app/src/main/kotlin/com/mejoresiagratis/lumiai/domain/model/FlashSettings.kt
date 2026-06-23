package com.mejoresiagratis.lumiai.domain.model

data class FlashSettings(
    val intensityLevel: Int = MAX_INTENSITY,
    val strobeHz: Float = 8f,
    val morseUnitMs: Long = 200L,
    val screenArgb: Int = -0x1,
    val morseText: String = "SOS",
    val screenBrightness: Float = 1f,
    val beaconIntervalMs: Long = 1500L,
    val beaconFlashMs: Long = 120L,
    val beaconAutoOffMin: Int = 0
) {
    fun coerced() = copy(
        intensityLevel = intensityLevel.coerceIn(MIN_INTENSITY, MAX_INTENSITY),
        strobeHz = strobeHz.coerceIn(MIN_STROBE_HZ, MAX_STROBE_HZ),
        morseUnitMs = morseUnitMs.coerceIn(MIN_UNIT_MS, MAX_UNIT_MS),
        morseText = morseText.take(MAX_MORSE_LEN),
        screenBrightness = screenBrightness.coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_SCREEN_BRIGHTNESS),
        beaconIntervalMs = beaconIntervalMs.coerceIn(MIN_BEACON_INTERVAL, MAX_BEACON_INTERVAL),
        beaconFlashMs = beaconFlashMs
            .coerceIn(MIN_BEACON_FLASH, MAX_BEACON_FLASH)
            .coerceAtMost(beaconIntervalMs.coerceIn(MIN_BEACON_INTERVAL, MAX_BEACON_INTERVAL) - 100L),
        beaconAutoOffMin = beaconAutoOffMin.coerceAtLeast(0)
    )

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 100
        const val MIN_STROBE_HZ = 1f
        const val MAX_STROBE_HZ = 20f
        const val MIN_UNIT_MS = 60L
        const val MAX_UNIT_MS = 400L
        const val MAX_MORSE_LEN = 50
        const val MIN_SCREEN_BRIGHTNESS = 0.05f
        const val MAX_SCREEN_BRIGHTNESS = 1f
        const val MIN_BEACON_INTERVAL = 400L
        const val MAX_BEACON_INTERVAL = 5000L
        const val MIN_BEACON_FLASH = 40L
        const val MAX_BEACON_FLASH = 1000L
    }
}
