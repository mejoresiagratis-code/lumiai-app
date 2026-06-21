package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashSettingsTest {
    @Test
    fun `coerced clamps every field into range`() {
        val s = FlashSettings(intensityLevel = 999, strobeHz = 99f, morseUnitMs = 5L).coerced()
        assertEquals(FlashSettings.MAX_INTENSITY, s.intensityLevel)
        assertEquals(FlashSettings.MAX_STROBE_HZ, s.strobeHz, 0f)
        assertEquals(FlashSettings.MIN_UNIT_MS, s.morseUnitMs)
    }
}
