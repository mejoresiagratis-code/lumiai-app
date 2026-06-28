package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessStateTest {

    @Test
    fun basic_siempre_desbloqueado() {
        assertTrue(AccessState().unlocks(Tier.BASIC))
    }

    @Test
    fun sin_permisos_ni_pro_advanced_y_ai_bloqueados() {
        val s = AccessState(Entitlements(hasAccount = false, hasSubscription = false), temporaryProActive = false)
        assertFalse(s.unlocks(Tier.ADVANCED))
        assertFalse(s.unlocks(Tier.AI))
    }

    @Test
    fun pro_temporal_desbloquea_advanced_y_ai() {
        val s = AccessState(Entitlements(hasAccount = false, hasSubscription = false), temporaryProActive = true)
        assertTrue(s.unlocks(Tier.BASIC))
        assertTrue(s.unlocks(Tier.ADVANCED))
        assertTrue(s.unlocks(Tier.AI))
    }

    @Test
    fun cuenta_desbloquea_advanced_pero_no_ai() {
        val s = AccessState(Entitlements(hasAccount = true), temporaryProActive = false)
        assertTrue(s.unlocks(Tier.ADVANCED))
        assertFalse(s.unlocks(Tier.AI))
    }

    @Test
    fun suscripcion_desbloquea_todo() {
        val s = AccessState(Entitlements(hasSubscription = true), temporaryProActive = false)
        assertTrue(s.unlocks(Tier.ADVANCED))
        assertTrue(s.unlocks(Tier.AI))
    }
}
