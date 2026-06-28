package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardProgressTest {

    @Test
    fun primer_anuncio_acumula_sin_conceder() {
        val out = RewardProgress.afterReward(0)
        assertEquals(1, out.newCount)
        assertFalse(out.grantsUnlock)
    }

    @Test
    fun alcanzar_el_umbral_concede_y_reinicia() {
        val out = RewardProgress.afterReward(RewardProgress.ADS_PER_GRANT - 1)
        assertEquals(0, out.newCount)
        assertTrue(out.grantsUnlock)
    }

    @Test
    fun contador_negativo_se_normaliza() {
        val out = RewardProgress.afterReward(-5)
        assertEquals(1, out.newCount)
        assertFalse(out.grantsUnlock)
    }
}
