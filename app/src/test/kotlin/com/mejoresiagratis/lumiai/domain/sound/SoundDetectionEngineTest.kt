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
    fun `por debajo del umbral nunca dispara (categoria sostenida)`() {
        // DESPERTADOR (sostenido, sin alivio): 0.4 < umbral MEDIA 0.5, nunca dispara.
        val e = engine()
        repeat(5) { i ->
            assertTrue(e.onWindow(mapOf("Alarm clock" to 0.4f), nowMs = i * 1000L).isEmpty())
        }
    }

    @Test
    fun `un transitorio dispara con score aliviado que a una sostenida no le bastaria`() {
        // Alivio del 30% para transitorios (QA 14-ago): un golpe de ~0.2 s llega con el
        // score diluido dentro de la ventana de ~1 s. TIMBRE umbral MEDIA 0.5 ->
        // efectivo 0.35: un 0.4 SI dispara (arriba, la sostenida con 0.4 no).
        val e = engine()
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.4f), 0L))
    }

    @Test
    fun `dispara solo tras alcanzar el debounce (categoria sostenida)`() {
        // DESPERTADOR no es transitorio: sigue exigiendo el debounce completo.
        val e = engine(debounce = 2)
        assertTrue(e.onWindow(mapOf("Alarm clock" to 0.9f), 0L).isEmpty())
        assertEquals(listOf(SoundCategory.DESPERTADOR), e.onWindow(mapOf("Alarm clock" to 0.9f), 500L))
    }

    @Test
    fun `una ventana por debajo rompe la racha (categoria sostenida)`() {
        val e = engine(debounce = 2)
        assertTrue(e.onWindow(mapOf("Alarm clock" to 0.9f), 0L).isEmpty())
        assertTrue(e.onWindow(mapOf("Alarm clock" to 0.1f), 500L).isEmpty()) // reset
        assertTrue(e.onWindow(mapOf("Alarm clock" to 0.9f), 1000L).isEmpty()) // racha 1
        assertEquals(listOf(SoundCategory.DESPERTADOR), e.onWindow(mapOf("Alarm clock" to 0.9f), 1500L))
    }

    @Test
    fun `un transitorio dispara con UNA sola ventana aunque el debounce sea mayor`() {
        // QA 14-ago: un ladrido/golpe/timbre dura menos que 2 ventanas (~1 s) — con el
        // debounce estandar jamas disparaba. Los transitorios exigen solo 1 ventana.
        val e = engine(debounce = 2)
        assertEquals(listOf(SoundCategory.TIMBRE), e.onWindow(mapOf("Doorbell" to 0.9f), 0L))
        assertEquals(listOf(SoundCategory.PERRO), e.onWindow(mapOf("Bark" to 0.9f), 100L))
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
    fun `mas sensibilidad baja el umbral necesario (categoria sostenida)`() {
        // DESPERTADOR (sostenido, sin alivio) con debounce=1 para aislar el umbral.
        // Con sensibilidad BAJA (umbral 0.7) un 0.5 no dispara...
        val baja = SoundAlertConfig().withSensitivity(SoundCategory.DESPERTADOR, Sensitivity.BAJA)
        val e1 = engine(config = baja, debounce = 1)
        assertTrue(e1.onWindow(mapOf("Alarm clock" to 0.5f), 0L).isEmpty())
        // ...pero con sensibilidad ALTA (umbral 0.3) si.
        val alta = SoundAlertConfig().withSensitivity(SoundCategory.DESPERTADOR, Sensitivity.ALTA)
        val e2 = engine(config = alta, debounce = 1)
        assertEquals(listOf(SoundCategory.DESPERTADOR), e2.onWindow(mapOf("Alarm clock" to 0.5f), 0L))
    }

    @Test
    fun `reset limpia rachas y cooldown`() {
        // DESPERTADOR (sostenido): TIMBRE ahora es transitorio y dispararia a la primera.
        val e = engine(debounce = 2)
        e.onWindow(mapOf("Alarm clock" to 0.9f), 0L) // racha 1
        e.reset()
        // tras reset hay que volver a acumular la racha completa
        assertTrue(e.onWindow(mapOf("Alarm clock" to 0.9f), 100L).isEmpty())
        assertEquals(listOf(SoundCategory.DESPERTADOR), e.onWindow(mapOf("Alarm clock" to 0.9f), 200L))
    }
}
