package com.mejoresiagratis.lumiai.domain.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundAlertConfigTest {

    @Test
    fun `v1 ofrece exactamente 8 categorias`() {
        assertEquals(8, SoundCategory.entries.size)
    }

    @Test
    fun `por defecto las 8 categorias estan activadas`() {
        val cfg = SoundAlertConfig()
        SoundCategory.entries.forEach { assertTrue("$it deberia venir activada", cfg.isEnabled(it)) }
        assertTrue(cfg.anyEnabled)
    }

    @Test
    fun `solo la alarma de humo es de seguridad`() {
        val safety = SoundCategory.entries.filter { it.safetyRelated }
        assertEquals(listOf(SoundCategory.ALARMA_HUMO), safety)
    }

    @Test
    fun `la categoria de seguridad arranca con sensibilidad alta`() {
        val cfg = SoundAlertConfig()
        assertEquals(Sensitivity.ALTA, cfg.sensitivity(SoundCategory.ALARMA_HUMO))
        assertEquals(Sensitivity.MEDIA, cfg.sensitivity(SoundCategory.TIMBRE))
    }

    @Test
    fun `el umbral refleja la sensibilidad`() {
        val cfg = SoundAlertConfig()
            .withSensitivity(SoundCategory.TIMBRE, Sensitivity.ALTA)
        assertEquals(0.3f, cfg.threshold(SoundCategory.TIMBRE))
        // mas sensibilidad implica umbral mas bajo
        assertTrue(Sensitivity.ALTA.scoreThreshold < Sensitivity.BAJA.scoreThreshold)
    }

    @Test
    fun `activeLabels une las etiquetas de las categorias activas`() {
        val cfg = SoundAlertConfig()
        val labels = cfg.activeLabels()
        assertTrue(labels.contains("Doorbell"))
        assertTrue(labels.contains("Baby cry, infant cry"))
        assertTrue(labels.contains("Smoke detector, smoke alarm"))
    }

    @Test
    fun `desactivar una categoria retira sus etiquetas de la allowlist`() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.PERRO, false)
        assertFalse(cfg.isEnabled(SoundCategory.PERRO))
        assertFalse(cfg.activeLabels().contains("Dog"))
        assertFalse(cfg.activeLabels().contains("Bark"))
        // otras siguen presentes
        assertTrue(cfg.activeLabels().contains("Doorbell"))
    }

    @Test
    fun `sin categorias activas la allowlist queda vacia`() {
        var cfg = SoundAlertConfig()
        SoundCategory.entries.forEach { cfg = cfg.withEnabled(it, false) }
        assertFalse(cfg.anyEnabled)
        assertTrue(cfg.activeLabels().isEmpty())
    }

    @Test
    fun `withEnabled y withSensitivity no mutan la config original`() {
        val original = SoundAlertConfig()
        original.withEnabled(SoundCategory.TIMBRE, false)
        original.withSensitivity(SoundCategory.TIMBRE, Sensitivity.BAJA)
        // la original permanece intacta
        assertTrue(original.isEnabled(SoundCategory.TIMBRE))
        assertEquals(Sensitivity.MEDIA, original.sensitivity(SoundCategory.TIMBRE))
    }

    @Test
    fun `ninguna categoria tiene etiquetas vacias`() {
        SoundCategory.entries.forEach {
            assertTrue("$it sin etiquetas", it.labels.isNotEmpty())
        }
    }
}
