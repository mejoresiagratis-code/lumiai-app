package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.domain.model.FlashSettings

object FlashSettingsMapper {
    const val KEY_INTENSITY = "intensity"
    const val KEY_STROBE_HZ = "strobe_hz"
    const val KEY_MORSE_UNIT = "morse_unit"
    const val KEY_SCREEN_ARGB = "screen_argb"

    fun toMap(s: FlashSettings): Map<String, Any> = mapOf(
        KEY_INTENSITY to s.intensityLevel,
        KEY_STROBE_HZ to s.strobeHz,
        KEY_MORSE_UNIT to s.morseUnitMs,
        KEY_SCREEN_ARGB to s.screenArgb
    )

    fun fromMap(m: Map<String, Any?>): FlashSettings {
        val d = FlashSettings()
        return FlashSettings(
            intensityLevel = (m[KEY_INTENSITY] as? Int) ?: d.intensityLevel,
            strobeHz = (m[KEY_STROBE_HZ] as? Float) ?: d.strobeHz,
            morseUnitMs = (m[KEY_MORSE_UNIT] as? Long) ?: d.morseUnitMs,
            screenArgb = (m[KEY_SCREEN_ARGB] as? Int) ?: d.screenArgb
        ).coerced()
    }
}
