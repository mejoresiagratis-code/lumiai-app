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
        assertEquals(Tier.AI, FlashMode.MUSIC.tier) // reclasificada 13-ago: PRO -> AI (admite anuncio)
    }

    @Test
    fun `suscribirse exige cuenta con sesion iniciada`() {
        assertFalse(canStartSubscriptionPurchase(hasAccount = false))
        assertTrue(canStartSubscriptionPurchase(hasAccount = true))
    }

    // ── canTryProByAd: probar Pro con anuncios exige cuenta CON correo verificado (13-ago) ──

    @Test
    fun `sin cuenta no puede probar pro por anuncio`() {
        assertFalse(Entitlements(hasAccount = false, isEmailVerified = false).canTryProByAd())
    }

    @Test
    fun `con cuenta pero sin verificar tampoco puede`() {
        assertFalse(Entitlements(hasAccount = true, isEmailVerified = false).canTryProByAd())
    }

    @Test
    fun `verificado sin cuenta sigue sin poder (caso imposible en la practica, pero la regla es AND)`() {
        assertFalse(Entitlements(hasAccount = false, isEmailVerified = true).canTryProByAd())
    }

    @Test
    fun `cuenta y correo verificado SI puede probar pro`() {
        assertTrue(Entitlements(hasAccount = true, isEmailVerified = true).canTryProByAd())
    }
}
