package com.mejoresiagratis.lumiai.data.sound

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the periodic task and makes synchronous capture failures observable exactly once. */
internal class ClassificationLoop(private val onError: (RuntimeException) -> Unit) {
    private val running = AtomicBoolean(false)
    private val executor = ScheduledThreadPoolExecutor(1)

    fun start(intervalMs: Long, classify: () -> Unit) {
        check(running.compareAndSet(false, true))
        executor.scheduleAtFixedRate({
            if (running.get()) {
                try {
                    classify()
                } catch (e: RuntimeException) {
                    if (running.compareAndSet(true, false)) {
                        executor.shutdown()
                        onError(e)
                    }
                }
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS)
    }

    /** Call after stopping AudioRecord to unblock capture; never from the capture thread. */
    fun awaitStopped() {
        while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            // Do not transfer microphone ownership while the old reader is still using it.
        }
    }

    fun stop() {
        running.set(false)
        executor.shutdownNow()
    }
}
