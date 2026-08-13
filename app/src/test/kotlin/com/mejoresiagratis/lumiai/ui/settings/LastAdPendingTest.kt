package com.mejoresiagratis.lumiai.ui.settings

import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cubre la lógica de [lastAdPending] que gobierna qué copy muestran los tres diálogos
 * de modo bloqueado (BeamHub, Alerta Sonora, Letrero LED) — añadida en v0.9.3 (Q3).
 *
 * La expresión real es:
 *   val lastAdPending = proUi.adsWatched >= proUi.adsPerGrant - 1 && !proUi.active
 *
 * Estos tests fijan su contrato sin tocar la UI (cero dependencias de Compose/Android).
 */
class LastAdPendingTest {

    private val ADS = RewardProgress.ADS_PER_GRANT   // 2 en producción

    /** Replica la expresión real del SettingsScreen/BeamHubScreen. */
    private fun lastAdPending(watched: Int, active: Boolean): Boolean =
        watched >= ADS - 1 && !active

    // ── Estado 0/2: ni de lejos, no activar urgencia ──────────────────────────
    @Test fun `sin anuncios y sin pro — no pendiente`() {
        assertFalse(lastAdPending(watched = 0, active = false))
    }

    // ── Estado 1/2 (el exactamente "Ya casi"): activar urgencia ──────────────
    @Test fun `un anuncio visto sin pro — pendiente`() {
        assertTrue(lastAdPending(watched = ADS - 1, active = false))
    }

    // ── Estado 2/2 completado PERO pro activo: la hora ya corre, no urgencia ──
    @Test fun `umbral alcanzado y pro activo — no pendiente`() {
        assertFalse(lastAdPending(watched = ADS, active = true))
    }

    // ── Contador en el umbral sin activar (edge-case: rewarded sin grant) ─────
    @Test fun `contador en umbral sin pro — pendiente`() {
        assertTrue(lastAdPending(watched = ADS, active = false))
    }

    // ── Pro activo cualquiera que sea el contador ──────────────────────────────
    @Test fun `pro activo desactiva la urgencia independientemente del contador`() {
        (0..ADS + 3).forEach { w ->
            assertFalse(
                "watched=$w con active=true debería ser false",
                lastAdPending(watched = w, active = true)
            )
        }
    }

    // ── Contadores negativos (no deberian ocurrir, pero la expresion es robusta) ─
    @Test fun `contador negativo no activa urgencia`() {
        assertFalse(lastAdPending(watched = -1, active = false))
    }
}
