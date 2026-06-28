package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryUnlockTest {

    private val now = 1_000_000L

    @Test
    fun activo_si_caducidad_futura() {
        assertTrue(TemporaryUnlock.isActive(now + 1, now))
    }

    @Test
    fun inactivo_si_caducidad_pasada_igual_o_cero() {
        assertFalse(TemporaryUnlock.isActive(now - 1, now))
        assertFalse(TemporaryUnlock.isActive(now, now)) // límite exacto: no activo
        assertFalse(TemporaryUnlock.isActive(0L, now))  // sin desbloqueo
    }

    @Test
    fun restante_nunca_negativo() {
        assertEquals(500L, TemporaryUnlock.remainingMillis(now + 500, now))
        assertEquals(0L, TemporaryUnlock.remainingMillis(now - 500, now))
        assertEquals(0L, TemporaryUnlock.remainingMillis(0L, now))
    }

    @Test
    fun extender_desde_cero_parte_de_ahora() {
        assertEquals(now + TemporaryUnlock.HOUR_MS, TemporaryUnlock.extended(0L, now, TemporaryUnlock.HOUR_MS))
    }

    @Test
    fun extender_desde_vigente_apila_sobre_la_caducidad() {
        val current = now + 10 * 60 * 1000L // +10 min, vigente
        assertEquals(current + TemporaryUnlock.HOUR_MS, TemporaryUnlock.extended(current, now, TemporaryUnlock.HOUR_MS))
    }

    @Test
    fun extender_desde_caducado_parte_de_ahora() {
        val current = now - 10 * 60 * 1000L // caducado
        assertEquals(now + TemporaryUnlock.HOUR_MS, TemporaryUnlock.extended(current, now, TemporaryUnlock.HOUR_MS))
    }
}
