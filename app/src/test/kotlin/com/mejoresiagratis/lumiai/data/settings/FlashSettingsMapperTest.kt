package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashSettingsMapperTest {
    @Test
    fun `map round-trips settings through a plain map`() {
        val original = FlashSettings(intensityLevel = 60, strobeHz = 12f, morseUnitMs = 150L, screenArgb = -0x10000)
        val restored = FlashSettingsMapper.fromMap(FlashSettingsMapper.toMap(original))
        assertEquals(original, restored)
    }

    @Test
    fun `fromMap falls back to defaults on missing keys`() {
        assertEquals(FlashSettings(), FlashSettingsMapper.fromMap(emptyMap()))
    }
}
