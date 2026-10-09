package com.mejoresiagratis.lumiai.domain.sound

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AlertOutputDeliveryTest {
    @Test fun flashOnlyNeverRequestsScreen() = runTest {
        var flashes = 0
        AlertChannel.FLASH.deliverOutputs(true, { fail("Unexpected screen") }, { flashes++ })
        assertEquals(1, flashes)
    }

    @Test fun screenOnlyNeverUsesFlash() = runTest {
        var screens = 0
        AlertChannel.PANTALLA.deliverOutputs(true, { screens++ }, { fail("Unexpected LED") })
        assertEquals(1, screens)
    }

    @Test fun deniedScreenDeliveryDoesNotFallBackToFlash() = runTest {
        var warning: String? = null
        AlertChannel.PANTALLA.deliverOutputs(
            true, { warning = "Permission missing: show notification/settings" },
            { fail("Screen must never be replaced by LED") }
        )
        assertNotNull(warning)
    }

    @Test fun bothRequestsBothOutputs() = runTest {
        val outputs = mutableListOf<String>()
        AlertChannel.AMBAS.deliverOutputs(true, { outputs += "screen" }, { outputs += "flash" })
        assertEquals(listOf("screen", "flash"), outputs)
    }

    @Test fun unavailableFlashDoesNotSubstituteScreen() = runTest {
        AlertChannel.FLASH.deliverOutputs(false, { fail("Unselected screen") }, { fail("Unavailable LED") })
    }

    @Test fun bothWithoutLedStillRequestsSelectedScreen() = runTest {
        var screens = 0
        AlertChannel.AMBAS.deliverOutputs(false, { screens++ }, { fail("Unavailable LED") })
        assertEquals(1, screens)
    }
}
