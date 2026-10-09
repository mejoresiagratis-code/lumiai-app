package com.mejoresiagratis.lumiai.data.session

import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.entitlement.whileAiAccess
import com.mejoresiagratis.lumiai.util.FakeTorchController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class HardwareSessionCoordinatorTest {
    @Test fun handoverWaitsForMicrophoneAndAllChildCleanup() = runTest {
        val sessions = HardwareSessionCoordinator(FakeTorchController())
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val first = launch {
            sessions.runSession(onFinished = { events += "finished" }) {
                launch {
                    try { awaitCancellation() } finally {
                        withContext(NonCancellable) {
                            events += "releasing"
                            release.await()
                            events += "released"
                        }
                    }
                }
                awaitCancellation()
            }
        }
        runCurrent()
        val next = launch {
            sessions.runSession(onStarted = { events += "next" }) { awaitCancellation() }
        }
        runCurrent()
        assertEquals(listOf("releasing"), events)
        release.complete(Unit)
        runCurrent()
        assertEquals(listOf("releasing", "released", "finished", "next"), events)
        assertTrue(first.isCompleted)
        next.cancelAndJoin()
    }

    @Test fun staleLeaseCannotTurnOffOrChangeNewOwner() = runTest {
        val hardware = FakeTorchController()
        val sessions = HardwareSessionCoordinator(hardware)
        lateinit var stale: TorchController
        val first = launch {
            sessions.runSession { torch ->
                stale = torch
                torch.turnOn(20)
                awaitCancellation()
            }
        }
        runCurrent()
        val next = launch {
            sessions.runSession { torch -> torch.turnOn(80); awaitCancellation() }
        }
        runCurrent()
        assertTrue(first.isCompleted)
        stale.turnOff()
        stale.pulseOff()
        stale.turnOn(10)
        assertTrue(hardware.isOn)
        assertEquals(80, hardware.lastIntensity)
        assertEquals(0, hardware.pulseOffCalls)
        next.cancelAndJoin()
        assertFalse(hardware.isOn)
    }

    @Test fun cancelledWaiterDoesNotOpenHardwareOrFinishAnotherSession() = runTest {
        val sessions = HardwareSessionCoordinator(FakeTorchController())
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val first = launch {
            sessions.runSession(onFinished = { events += "first finished" }) {
                try { awaitCancellation() } finally { withContext(NonCancellable) { release.await() } }
            }
        }
        runCurrent()
        val waiter = launch {
            sessions.runSession(
                onStarted = { events += "waiter started" },
                onFinished = { events += "waiter finished" }
            ) { awaitCancellation() }
        }
        runCurrent()
        waiter.cancelAndJoin()
        assertTrue(events.isEmpty())
        release.complete(Unit)
        first.join()
        sessions.runSession(onStarted = { events += "last" }) {}
        assertEquals(listOf("first finished", "last"), events)
    }

    @Test fun rapidRequestsKeepOneOwnerAndLeaveLatestActive() = runTest {
        val sessions = HardwareSessionCoordinator(FakeTorchController())
        var active = 0
        var maximum = 0
        var last = -1
        val jobs = (0..19).map { index ->
            launch {
                sessions.runSession(
                    onStarted = { active++; maximum = maxOf(maximum, active); last = index },
                    onFinished = { active-- }
                ) { awaitCancellation() }
            }
        }
        runCurrent()
        assertEquals(1, maximum)
        assertEquals(1, active)
        assertEquals(19, last)
        assertTrue(jobs.dropLast(1).all { it.isCompleted })
        jobs.last().cancelAndJoin()
        assertEquals(0, active)
    }

    @Test fun completionAndFailureReleaseHardwareAndUiState() = runTest {
        val hardware = FakeTorchController()
        val sessions = HardwareSessionCoordinator(hardware)
        var on = false
        try {
            sessions.runSession(onStarted = { on = true }, onFinished = { on = false }) {
                it.turnOn(50)
                throw IllegalStateException("Capture failed")
            }
            fail("Expected capture failure")
        } catch (_: IllegalStateException) { }
        assertFalse(on)
        assertFalse(hardware.isOn)
        sessions.runSession { it.turnOn(60) }
        assertFalse(hardware.isOn)
    }

    @Test fun oldUiCleanupPrecedesNewUiStart() = runTest {
        val sessions = HardwareSessionCoordinator(FakeTorchController())
        var on = false
        val first = launch {
            sessions.runSession(onStarted = { on = true }, onFinished = { on = false }) {
                awaitCancellation()
            }
        }
        runCurrent()
        val next = launch {
            sessions.runSession(onStarted = { on = true }, onFinished = { on = false }) {
                awaitCancellation()
            }
        }
        runCurrent()
        first.join()
        assertTrue(on)
        next.cancelAndJoin()
        assertFalse(on)
    }

    @Test fun handoverCancelsAccessCollectionInsteadOfLeavingServiceAlive() = runTest {
        val sessions = HardwareSessionCoordinator(FakeTorchController())
        val access = MutableStateFlow(true)
        var stopped = false
        val first = launch {
            try {
                sessions.runSession {
                    access.whileAiAccess({}) { awaitCancellation() }
                }
            } finally { stopped = true }
        }
        runCurrent()
        val next = launch { sessions.runSession { awaitCancellation() } }
        runCurrent()
        assertTrue(first.isCompleted)
        assertTrue(stopped)
        next.cancelAndJoin()
    }

    @Test fun accessRevocationReleasesBeforeAnotherSessionStarts() = runTest {
        val hardware = FakeTorchController()
        val sessions = HardwareSessionCoordinator(hardware)
        val access = MutableStateFlow(true)
        val events = mutableListOf<String>()
        val job = launch {
            sessions.runSession(onFinished = { events += "finished" }) { torch ->
                access.whileAiAccess({ events += "denied" }) {
                    torch.turnOn(50)
                    try { awaitCancellation() } finally { events += "released" }
                }
            }
        }
        runCurrent()
        access.value = false
        runCurrent()
        assertTrue(job.isCompleted)
        assertFalse(hardware.isOn)
        assertEquals(listOf("released", "denied", "finished"), events)
    }
}
