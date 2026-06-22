package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashSettingsTest {
    @Test fun `brillo de pantalla se acota`() {
        assertEquals(FlashSettings.MAX_SCREEN_BRIGHTNESS, FlashSettings(screenBrightness = 5f).coerced().screenBrightness, 0f)
        assertEquals(FlashSettings.MIN_SCREEN_BRIGHTNESS, FlashSettings(screenBrightness = -1f).coerced().screenBrightness, 0f)
    }
    @Test fun `mensaje morse se limita en longitud`() {
        val long = "A".repeat(200)
        assertEquals(FlashSettings.MAX_MORSE_LEN, FlashSettings(morseText = long).coerced().morseText.length)
    }
}
