package com.mejoresiagratis.lumiai.domain.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundDetectionEngineTest {

    // ---------- SoundLabelMatcher ----------

    @Test
    fun `el matcher resuelve etiquetas a su categoria`() {
        val m = SoundLabelMatcher()
        assertEquals(SoundCategory.TIMBRE, m.categoryFor("Doorbell"))
        assertEquals(SoundCategory.PERRO, m.categoryFor("Bark"))
        assertEquals(SoundCategory.ALARMA_HUMO, m.categoryFor("Smoke detector, smoke alarm"))
    }

    @Test
    fun `etiqueta desconocida no tiene categoria`() {
        assertNull(SoundLabelMatcher().categoryFor("Guitar"))
    }

    @Test
    fun `ninguna etiqueta pertenece a dos categorias`() {
        val totalLabels = SoundCategory.entries.sumOf { it.labels.size }
        assertEquals(totalLabels, SoundLabelMatcher().knownLabels.size)
    }

    // ---------- SoundDetectionEngine ----------

    private fun engine(
        config: SoundAlertConfig = SoundAlertConfig(),
        debounce: Int = 2,
        cooldown: Long = 8_000L
    ) = SoundDetectionEngine(config, debounceWindows = debounce, cooldownMs = cooldown)

    @Test
    fun `por debajo del umbral nunca dispara`() {
        val e = engine()
        // TIMBRE umbral MEDIA = 0.5
        repeat(5) { i ->
            assertTrue(e.onWindow(mapOf("Doorbell" to 0.4f), nowMs = i * 1000L).isEmpty())
        }
    }

    @Test
    fun `dispara solo tras alcanzar el debounce`() {
        val e = engine(debounce = 2)
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 0L).isEmpty())
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 500L))
    }

    @Test
    fun `una ventana por debajo rompe la racha`() {
        val e = engine(debounce = 2)
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 0L).isEmpty())
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.1f), 500L).isEmpty()) // reset
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 1000L).isEmpty()) // racha 1
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 1500L))
    }

    @Test
    fun `el cooldown suprime disparos repetidos y luego vuelve a permitir`() {
        val e = engine(debounce = 1, cooldown = 8_000L)
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 0L))
        // dentro del cooldown: nada
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 3_000L).isEmpty())
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 7_999L).isEmpty())
        // pasado el cooldown: vuelve a disparar
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 8_000L))
    }

    @Test
    fun `una categoria desactivada nunca dispara`() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.PERRO, false)
        val e = engine(config = cfg, debounce = 1)
        assertTrue(e.onWindow(mapOf("Bark" to 0.99f), 0L).isEmpty())
        // otra activa si dispara en la misma ventana
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.99f), 1000L))
    }

    @Test
    fun `categorias distintas son independientes`() {
        val e = engine(debounce = 1)
        val fired = e.onWindow(mapOf("Doorbell" to 0.9f, "Bark" to 0.9f), 0L).toSet()
        assertEquals(setOf(SoundCategory.TIMBRE, SoundCategory.PERRO), fired)
    }

    @Test
    fun `mas sensibilidad baja el umbral necesario`() {
        // Con sensibilidad BAJA (umbral 0.7) un 0.5 no dispara...
        val baja = SoundAlertConfig().withSensitivity(SoundCategory.TIMBRE, Sensitivity.BAJA)
        val e1 = engine(config = baja, debounce = 1)
        assertTrue(e1.onWindow(mapOf("Doorbell" to 0.5f), 0L).isEmpty())
        // ...pero con sensibilidad ALTA (umbral 0.3) si.
        val alta = SoundAlertConfig().withSensitivity(SoundCategory.TIMBRE, Sensitivity.ALTA)
        val e2 = engine(config = alta, debounce = 1)
        assertEquals(listOf(SoundCategory.TIMBRE), e2.onWindow(mapOf("Doorbell" to 0.5f), 0L))
    }

    @Test
    fun `reset limpia rachas y cooldown`() {
        val e = engine(debounce = 2)
        e.onWindow(mapOf("Doorbell" to 0.9f), 0L) // racha 1
        e.reset()
        // tras reset hay que volver a acumular la racha completa
        assertTrue(e.onWindow(mapOf("Doorbell" to 0.9f), 100L).isEmpty())
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 200L))
    }
}
