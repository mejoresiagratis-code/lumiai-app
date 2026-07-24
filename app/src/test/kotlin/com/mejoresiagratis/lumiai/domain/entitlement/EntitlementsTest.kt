package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {

    @Test
    fun `basic siempre desbloqueado`() {
        assertTrue(Entitlements().unlocks(Tier.BASIC))
    }

    @Test
    fun `advanced requiere cuenta o suscripcion`() {
        assertFalse(Entitlements(hasAccount = false, hasSubscription = false).unlocks(Tier.ADVANCED))
        assertTrue(Entitlements(hasAccount = true).unlocks(Tier.ADVANCED))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.ADVANCED))
    }

    @Test
    fun `solo continuo y pantalla son basic`() {
        assertEquals(Tier.BASIC, FlashMode.CONTINUOUS.tier)
        assertEquals(Tier.BASIC, FlashMode.SCREEN.tier)
    }

    @Test
    fun `sos estrobo baliza y morse son advanced`() {
        assertEquals(Tier.ADVANCED, FlashMode.SOS_MORSE.tier)
        assertEquals(Tier.ADVANCED, FlashMode.STROBE.tier)
        assertEquals(Tier.ADVANCED, FlashMode.BEACON.tier)
        assertEquals(Tier.ADVANCED, FlashMode.TEXT_MORSE.tier)
    }

    @Test
    fun `ai solo con suscripcion`() {
        assertFalse(Entitlements(hasAccount = true).unlocks(Tier.AI))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.AI))
    }
    @Test
    fun `pro estricto solo con suscripcion y musica es pro`() {
        assertFalse(Entitlements(hasAccount = true).unlocks(Tier.PRO))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.PRO))
        assertEquals(Tier.PRO, FlashMode.MUSIC.tier)
    }
}
