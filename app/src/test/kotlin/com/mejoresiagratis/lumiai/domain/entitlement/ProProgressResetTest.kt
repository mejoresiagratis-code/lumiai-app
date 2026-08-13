package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProProgressResetTest {

    @Test
    fun instalacion_inicial_sin_version_guardada_no_reinicia() {
        assertFalse(ProProgressReset.shouldReset(lastVersionCode = null, currentVersionCode = 41))
    }

    @Test
    fun mismo_arranque_misma_version_no_reinicia() {
        assertFalse(ProProgressReset.shouldReset(lastVersionCode = 41, currentVersionCode = 41))
    }

    @Test
    fun version_distinta_actualizacion_SI_reinicia() {
        assertTrue(ProProgressReset.shouldReset(lastVersionCode = 41, currentVersionCode = 44))
    }

    @Test
    fun tambien_reinicia_si_el_versionCode_bajara_reinstalacion_manual() {
        // No debería ocurrir en producción (versionCode solo sube), pero la regla es
        // simétrica: cualquier discrepancia cuenta como "versión distinta".
        assertTrue(ProProgressReset.shouldReset(lastVersionCode = 44, currentVersionCode = 41))
    }
}
