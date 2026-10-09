package com.mejoresiagratis.lumiai.data.sound

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class ClassificationLoopTest {
    @Test fun periodicReadFailureIsReportedExactlyOnce() {
        val failures = AtomicInteger()
        val attempts = AtomicInteger()
        val reported = CountDownLatch(1)
        val loop = ClassificationLoop {
            failures.incrementAndGet()
            reported.countDown()
        }
        try {
            loop.start(1) {
                if (attempts.incrementAndGet() == 2) throw IllegalStateException("AudioRecord failed")
            }
            assertTrue("Failure must reach service", reported.await(3, TimeUnit.SECONDS))
            assertEquals(1, failures.get())
            assertEquals(2, attempts.get())
        } finally { loop.stop() }
    }

    @Test fun shutdownWaitsForCaptureCleanupBeforeReturning() {
        val entered = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val joined = CountDownLatch(1)
        val failures = AtomicInteger()
        val loop = ClassificationLoop { failures.incrementAndGet() }
        val joiner = Thread { loop.awaitStopped(); joined.countDown() }
        try {
            loop.start(1) {
                entered.countDown()
                try {
                    CountDownLatch(1).await()
                } catch (_: InterruptedException) {
                    interrupted.countDown()
                    release.await(3, TimeUnit.SECONDS)
                }
            }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            loop.stop()
            assertTrue(interrupted.await(3, TimeUnit.SECONDS))
            joiner.start()
            assertFalse("Must wait for the recorder cleanup", joined.await(100, TimeUnit.MILLISECONDS))
            release.countDown()
            assertTrue(joined.await(3, TimeUnit.SECONDS))
            assertEquals(0, failures.get())
        } finally {
            release.countDown()
            loop.stop()
            joiner.join(3_000)
        }
    }

    @Test fun stoppingDuringReadDoesNotReportAnError() {
        val entered = CountDownLatch(1)
        val exited = CountDownLatch(1)
        val failures = AtomicInteger()
        val loop = ClassificationLoop { failures.incrementAndGet() }
        try {
            loop.start(1) {
                entered.countDown()
                try {
                    CountDownLatch(1).await()
                } catch (_: InterruptedException) {
                    throw IllegalStateException("Capture stopped")
                } finally { exited.countDown() }
            }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            loop.stop()
            assertTrue(exited.await(3, TimeUnit.SECONDS))
            assertEquals(0, failures.get())
        } finally { loop.stop() }
    }
}
