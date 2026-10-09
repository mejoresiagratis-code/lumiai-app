package com.mejoresiagratis.lumiai.domain.entitlement

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AiAccessSessionTest {
    @Test fun initiallyDeniedNeverStartsHardware() = runTest {
        var started = false
        var denied = 0
        flowOf(false).whileAiAccess({ denied++ }) { started = true }
        assertFalse(started)
        assertEquals(1, denied)
    }

    @Test fun revocationCancelsWorkBeforeReportingDenial() = runTest {
        val access = MutableStateFlow(true)
        val events = mutableListOf<String>()
        val job = launch {
            access.whileAiAccess({ events += "denied" }) {
                events += "start"
                try { awaitCancellation() } finally { events += "stop" }
            }
        }
        runCurrent()
        access.value = false
        runCurrent()
        assertEquals(listOf("start", "stop", "denied"), events)
        assertTrue(job.isCompleted)
    }

    @Test fun repeatedGrantedStateDoesNotRestartSession() = runTest {
        var starts = 0
        var denials = 0
        flowOf(true, true, false).whileAiAccess({ denials++ }) { starts++ }
        assertEquals(1, starts)
        assertEquals(1, denials)
    }
}
