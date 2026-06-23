package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.domain.model.FlashSettings

object FlashSettingsMapper {
    const val KEY_INTENSITY = "intensity"
    const val KEY_STROBE_HZ = "strobe_hz"
    const val KEY_MORSE_UNIT = "morse_unit"
    const val KEY_SCREEN_ARGB = "screen_argb"
    const val KEY_MORSE_TEXT = "morse_text"
    const val KEY_SCREEN_BRIGHTNESS = "screen_brightness"
    const val KEY_BEACON_INTERVAL = "beacon_interval"
    const val KEY_BEACON_FLASH = "beacon_flash"
    const val KEY_BEACON_AUTOOFF = "beacon_autooff"

    fun toMap(s: FlashSettings): Map<String, Any> = mapOf(
        KEY_INTENSITY to s.intensityLevel,
        KEY_STROBE_HZ to s.strobeHz,
        KEY_MORSE_UNIT to s.morseUnitMs,
        KEY_SCREEN_ARGB to s.screenArgb,
        KEY_MORSE_TEXT to s.morseText,
        KEY_SCREEN_BRIGHTNESS to s.screenBrightness,
        KEY_BEACON_INTERVAL to s.beaconIntervalMs,
        KEY_BEACON_FLASH to s.beaconFlashMs,
        KEY_BEACON_AUTOOFF to s.beaconAutoOffMin
    )

    fun fromMap(m: Map<String, Any?>): FlashSettings {
        val d = FlashSettings()
        return FlashSettings(
            intensityLevel = (m[KEY_INTENSITY] as? Int) ?: d.intensityLevel,
            strobeHz = (m[KEY_STROBE_HZ] as? Float) ?: d.strobeHz,
            morseUnitMs = (m[KEY_MORSE_UNIT] as? Long) ?: d.morseUnitMs,
            screenArgb = (m[KEY_SCREEN_ARGB] as? Int) ?: d.screenArgb,
            morseText = (m[KEY_MORSE_TEXT] as? String) ?: d.morseText,
            screenBrightness = (m[KEY_SCREEN_BRIGHTNESS] as? Float) ?: d.screenBrightness,
            beaconIntervalMs = (m[KEY_BEACON_INTERVAL] as? Long) ?: d.beaconIntervalMs,
            beaconFlashMs = (m[KEY_BEACON_FLASH] as? Long) ?: d.beaconFlashMs,
            beaconAutoOffMin = (m[KEY_BEACON_AUTOOFF] as? Int) ?: d.beaconAutoOffMin
        ).coerced()
    }
}
