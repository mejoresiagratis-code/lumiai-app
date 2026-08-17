package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    @Test
    fun `azul y naranja libres para todos incluso sin cuenta`() {
        assertTrue(AccentColor.BLUE.isUnlocked(hasAccount = false, hasPro = false))
        assertTrue(AccentColor.ORANGE.isUnlocked(hasAccount = false, hasPro = false))
    }

    @Test
    fun `solidos de cuenta bloqueados sin login y libres con cuenta o pro`() {
        val gated = listOf(
            AccentColor.AMBER, AccentColor.YELLOW, AccentColor.GREEN,
            AccentColor.RED, AccentColor.VIOLET, AccentColor.WHITE
        )
        gated.forEach { ac ->
            assertFalse(ac.name, ac.isUnlocked(hasAccount = false, hasPro = false))
            assertTrue(ac.name, ac.isUnlocked(hasAccount = true, hasPro = false))
            assertTrue(ac.name, ac.isUnlocked(hasAccount = false, hasPro = true))
        }
    }

    @Test
    fun `multicolor exige acceso pro, no basta con tener cuenta`() {
        assertFalse(AccentColor.MULTICOLOR.isUnlocked(hasAccount = true, hasPro = false))
        assertTrue(AccentColor.MULTICOLOR.isUnlocked(hasAccount = false, hasPro = true))
    }

    @Test
    fun `multicolor se desbloquea con el acceso pro TEMPORAL, no solo con suscripcion`() {
        // Revisión del 17-ago: hasPro es el acceso EFECTIVO — suscripción o desbloqueo
        // temporal por anuncios. La regla pura no distingue de dónde viene el Pro, que es
        // justo lo que hace que multicolor se comporte ya como el Letrero LED.
        assertTrue(AccentColor.MULTICOLOR.isUnlocked(hasAccount = true, hasPro = true))
    }

    @Test
    fun `orden de swatches azul y naranja primero multicolor ultimo`() {
        val order = AccentColor.entries
        assertEquals(AccentColor.BLUE, order.first())
        assertEquals(AccentColor.ORANGE, order[1])
        assertEquals(AccentColor.MULTICOLOR, order.last())
    }
}
