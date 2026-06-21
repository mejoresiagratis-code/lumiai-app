package com.mejoresiagratis.lumiai.domain.entitlement

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
    fun `ai solo con suscripcion`() {
        assertFalse(Entitlements(hasAccount = true).unlocks(Tier.AI))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.AI))
    }
}
