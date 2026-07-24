package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    @Test
    fun `azul y naranja libres para todos incluso sin cuenta`() {
        assertTrue(AccentColor.BLUE.isUnlocked(hasAccount = false, hasSubscription = false))
        assertTrue(AccentColor.ORANGE.isUnlocked(hasAccount = false, hasSubscription = false))
    }

    @Test
    fun `solidos de cuenta bloqueados sin login y libres con cuenta o pro`() {
        val gated = listOf(
            AccentColor.AMBER, AccentColor.YELLOW, AccentColor.GREEN,
            AccentColor.RED, AccentColor.VIOLET, AccentColor.WHITE
        )
        gated.forEach { ac ->
            assertFalse(ac.name, ac.isUnlocked(hasAccount = false, hasSubscription = false))
            assertTrue(ac.name, ac.isUnlocked(hasAccount = true, hasSubscription = false))
            assertTrue(ac.name, ac.isUnlocked(hasAccount = false, hasSubscription = true))
        }
    }

    @Test
    fun `multicolor exclusivo de suscripcion pro`() {
        assertFalse(AccentColor.MULTICOLOR.isUnlocked(hasAccount = true, hasSubscription = false))
        assertTrue(AccentColor.MULTICOLOR.isUnlocked(hasAccount = false, hasSubscription = true))
    }

    @Test
    fun `orden de swatches azul y naranja primero multicolor ultimo`() {
        val order = AccentColor.entries
        assertEquals(AccentColor.BLUE, order.first())
        assertEquals(AccentColor.ORANGE, order[1])
        assertEquals(AccentColor.MULTICOLOR, order.last())
    }
}
