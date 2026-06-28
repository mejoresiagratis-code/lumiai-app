package com.mejoresiagratis.lumiai.domain.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundAlertCodecAndFlashTest {

    // ---------- Codec ----------

    @Test
    fun `encode y decode son simetricos para la config por defecto`() {
        val original = SoundAlertConfig()
        val restored = SoundAlertConfigCodec.decode(SoundAlertConfigCodec.encode(original))
        SoundCategory.entries.forEach {
            assertEquals(original.isEnabled(it), restored.isEnabled(it))
            assertEquals(original.sensitivity(it), restored.sensitivity(it))
        }
    }

    @Test
    fun `decode conserva los cambios del usuario`() {
        val cfg = SoundAlertConfig()
            .withEnabled(SoundCategory.PERRO, false)
            .withSensitivity(SoundCategory.TIMBRE, Sensitivity.ALTA)
        val restored = SoundAlertConfigCodec.decode(SoundAlertConfigCodec.encode(cfg))
        assertFalse(restored.isEnabled(SoundCategory.PERRO))
        assertEquals(Sensitivity.ALTA, restored.sensitivity(SoundCategory.TIMBRE))
    }

    @Test
    fun `decode de null o vacio devuelve los valores por defecto`() {
        assertTrue(SoundAlertConfigCodec.decode(null).isEnabled(SoundCategory.TIMBRE))
        assertTrue(SoundAlertConfigCodec.decode("").anyEnabled)
    }

    @Test
    fun `decode ignora entradas corruptas y desconocidas y conserva el resto`() {
        // categoria desconocida, campos de mas, sensibilidad invalida -> se ignoran/saltan
        val raw = "DESCONOCIDA:1:MEDIA;PERRO:0;TIMBRE:0:NOEXISTE;BEBE:0:ALTA"
        val cfg = SoundAlertConfigCodec.decode(raw)
        // BEBE bien formada se aplica
        assertFalse(cfg.isEnabled(SoundCategory.BEBE))
        assertEquals(Sensitivity.ALTA, cfg.sensitivity(SoundCategory.BEBE))
        // PERRO (2 campos) se ignora -> queda por defecto (activa)
        assertTrue(cfg.isEnabled(SoundCategory.PERRO))
        // TIMBRE con sensibilidad invalida: se ignora la entrada por completo -> default activa/MEDIA
        assertTrue(cfg.isEnabled(SoundCategory.TIMBRE))
    }

    // ---------- Patrones de destello ----------

    @Test
    fun `cada categoria tiene un patron no vacio y de longitud par`() {
        SoundCategory.entries.forEach {
            val p = SoundAlertFlash.patternFor(it)
            assertTrue("$it patron vacio", p.isNotEmpty())
            assertEquals("$it longitud impar", 0, p.size % 2)
            assertTrue("$it tiene duraciones <= 0", p.all { ms -> ms > 0 })
        }
    }

    @Test
    fun `los patrones son distinguibles entre categorias`() {
        val distinct = SoundCategory.entries.map { SoundAlertFlash.patternFor(it).toList() }.toSet()
        assertEquals(SoundCategory.entries.size, distinct.size)
    }
}
