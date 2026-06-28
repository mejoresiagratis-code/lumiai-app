package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F0 del modo Alerta Sonora: contrato de capacidad de microfono + regla de gating.
 * El modo aun no existe; aqui se fija la regla pura para que, cuando aterrice y declare
 * requiresMicrophone()=true, quede oculto en dispositivos sin microfono.
 */
class ModeAvailabilityTest {

    private fun caps(flash: Boolean = true, mic: Boolean = true) =
        DeviceCapabilities(hasFlash = flash, maxTorchLevel = 1, hasMicrophone = mic)

    // --- Regla pura: las cuatro combinaciones de exigencia x dos capacidades ---

    @Test
    fun `modo sin exigencias siempre disponible`() {
        assertTrue(modeAvailable(requiresFlash = false, requiresMicrophone = false, caps(flash = false, mic = false)))
    }

    @Test
    fun `modo que exige flash oculto sin flash`() {
        assertFalse(modeAvailable(requiresFlash = true, requiresMicrophone = false, caps(flash = false)))
        assertTrue(modeAvailable(requiresFlash = true, requiresMicrophone = false, caps(flash = true)))
    }

    @Test
    fun `modo que exige microfono oculto sin microfono`() {
        assertFalse(modeAvailable(requiresFlash = false, requiresMicrophone = true, caps(mic = false)))
        assertTrue(modeAvailable(requiresFlash = false, requiresMicrophone = true, caps(mic = true)))
    }

    @Test
    fun `modo que exige flash y microfono necesita ambos`() {
        assertFalse(modeAvailable(requiresFlash = true, requiresMicrophone = true, caps(flash = true, mic = false)))
        assertFalse(modeAvailable(requiresFlash = true, requiresMicrophone = true, caps(flash = false, mic = true)))
        assertTrue(modeAvailable(requiresFlash = true, requiresMicrophone = true, caps(flash = true, mic = true)))
    }

    // --- Contrato actual del enum: hoy ningun modo exige microfono ---

    @Test
    fun `ningun modo actual requiere microfono`() {
        FlashMode.entries.forEach { mode ->
            assertFalse("'$mode' no deberia requerir microfono aun", mode.requiresMicrophone())
        }
    }

    @Test
    fun `la presencia de microfono no cambia la disponibilidad de los modos actuales`() {
        FlashMode.entries.forEach { mode ->
            val conMic = mode.isAvailable(caps(flash = true, mic = true))
            val sinMic = mode.isAvailable(caps(flash = true, mic = false))
            assertTrue("'$mode' no debe depender del microfono todavia", conMic == sinMic)
        }
    }
}
