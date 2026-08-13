package com.mejoresiagratis.lumiai.domain.flash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfOffWindowTest {

    @Test
    fun evento_inmediatamente_despues_de_nuestro_apagado_es_propio() {
        assertTrue(SelfOffWindow.isOwnOff(lastSelfOffAtMs = 1_000L, nowMs = 1_010L))
    }

    @Test
    fun evento_justo_en_el_borde_de_la_ventana_sigue_siendo_propio() {
        assertTrue(SelfOffWindow.isOwnOff(lastSelfOffAtMs = 1_000L, nowMs = 1_400L, graceMs = 400L))
    }

    @Test
    fun evento_pasada_la_ventana_es_externo() {
        assertFalse(SelfOffWindow.isOwnOff(lastSelfOffAtMs = 1_000L, nowMs = 1_401L, graceMs = 400L))
    }

    @Test
    fun sin_apagado_propio_reciente_cualquier_evento_es_externo() {
        // lastSelfOffAtMs = 0L representa "nunca hemos apagado nosotros mismos".
        assertFalse(SelfOffWindow.isOwnOff(lastSelfOffAtMs = 0L, nowMs = 5_000L))
    }
}
