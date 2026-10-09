package com.mejoresiagratis.lumiai.data.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PurchaseAcknowledgementTest {
    @Test fun acknowledgedPurchaseDoesNotCallPlayAgain() = runTest {
        assertTrue(PurchaseAcknowledgement().confirm(true) { error("Unexpected call") })
    }

    @Test fun temporaryFailureIsRetriedBeforeSuccess() = runTest {
        var attempts = 0
        val confirmed = PurchaseAcknowledgement().confirm(false) { ++attempts == 3 }
        assertTrue(confirmed)
        assertEquals(3, attempts)
        assertEquals(3_000L, testScheduler.currentTime)
    }

    @Test fun repeatedFailureDoesNotGrantAccess() = runTest {
        var attempts = 0
        assertFalse(PurchaseAcknowledgement().confirm(false) { attempts++; false })
        assertEquals(3, attempts)
    }

    @Test fun thrownTransportFailureCanRecover() = runTest {
        var attempts = 0
        assertTrue(PurchaseAcknowledgement().confirm(false) {
            if (++attempts == 1) throw IllegalStateException("Disconnected")
            true
        })
        assertEquals(2, attempts)
    }

    @Test fun cancellationIsNotRetriedOrConvertedToSuccess() = runTest {
        var attempts = 0
        try {
            PurchaseAcknowledgement().confirm(false) { attempts++; throw CancellationException() }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, attempts)
        }
    }
}
