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
    @Test fun `minutos de sueno del modo intimo nunca son negativos`() {
        assertEquals(0, FlashSettings(intimateSleepMinutes = -5).coerced().intimateSleepMinutes)
        assertEquals(30, FlashSettings(intimateSleepMinutes = 30).coerced().intimateSleepMinutes)
    }
    @Test fun `techo de brillo del modo intimo es mas bajo que el general`() {
        assertEquals(0.40f, FlashSettings.MAX_INTIMATE_BRIGHTNESS, 0f)
        assertEquals(true, FlashSettings.MAX_INTIMATE_BRIGHTNESS < FlashSettings.MAX_SCREEN_BRIGHTNESS)
    }
    @Test fun `default del modo intimo es apagado con atmosfera vela y respiracion`() {
        val d = FlashSettings()
        assertEquals(false, d.intimateEnabled)
        assertEquals(ScreenAtmosphere.VELA, d.intimateAtmosphere)
        assertEquals(ScreenAnimation.RESPIRACION, d.intimateAnimation)
    }
}
