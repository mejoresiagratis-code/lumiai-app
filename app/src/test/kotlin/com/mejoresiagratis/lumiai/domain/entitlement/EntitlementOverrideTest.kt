package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementOverrideTest {

    @Test
    fun sin_forzar_respeta_el_valor_real() {
        val base = Entitlements(hasAccount = true, hasSubscription = false)
        assertEquals(base, EntitlementOverride().apply(base))
    }

    @Test
    fun forzar_suscripcion_a_true_anula_la_real() {
        val base = Entitlements(hasAccount = false, hasSubscription = false)
        val out = EntitlementOverride(forceSubscription = true).apply(base)
        assertEquals(Entitlements(hasAccount = false, hasSubscription = true), out)
    }

    @Test
    fun forzar_cuenta_a_false_anula_la_real() {
        val base = Entitlements(hasAccount = true, hasSubscription = true)
        val out = EntitlementOverride(forceAccount = false).apply(base)
        assertEquals(Entitlements(hasAccount = false, hasSubscription = true), out)
    }

    @Test
    fun forzar_ambos_campos() {
        val out = EntitlementOverride(forceAccount = true, forceSubscription = true)
            .apply(Entitlements())
        assertEquals(Entitlements(hasAccount = true, hasSubscription = true), out)
    }
}
