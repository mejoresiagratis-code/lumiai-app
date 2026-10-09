package com.mejoresiagratis.lumiai.data.session

import com.mejoresiagratis.lumiai.data.torch.TorchController
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Last acquired session wins. Every service must capture audio inside [runSession] and
 * use its leased torch. Handover waits for all children AND resource cleanup to finish.
 * Android's asynchronous stopService/onDestroy is not a hardware ownership boundary.
 */
@Singleton
class HardwareSessionCoordinator @Inject constructor(private val hardware: TorchController) {
    private val handover = Mutex()
    private val guard = Any()
    private var active: Session? = null

    suspend fun runSession(
        onStarted: () -> Unit = {},
        onFinished: () -> Unit = {},
        block: suspend CoroutineScope.(TorchController) -> Unit
    ) = coroutineScope {
        val session = Session(coroutineContext.job)
        try {
            handover.withLock {
                val previous = synchronized(guard) { active?.job }
                previous?.cancelAndJoin()
                ensureActive()
                synchronized(guard) {
                    active = session
                    onStarted()
                }
            }
            ensureActive()
            // This inner scope joins cancelled children before the ownership finalizer.
            coroutineScope { block(LeasedTorch(session)) }
        } finally {
            // Never acquire handover here: the next owner holds it while joining us.
            synchronized(guard) {
                if (active === session) {
                    active = null
                    try { hardware.turnOff() } finally { onFinished() }
                }
            }
        }
    }

    private class Session(val job: Job)

    private inner class LeasedTorch(private val owner: Session) : TorchController {
        override val hasFlash get() = hardware.hasFlash
        override val maxIntensityLevel get() = hardware.maxIntensityLevel
        override val externalOffEvents = hardware.externalOffEvents.filter {
            synchronized(guard) { active === owner && owner.job.isActive }
        }

        override fun turnOn(intensityLevel: Int) = synchronized(guard) {
            if (active === owner && owner.job.isActive) hardware.turnOn(intensityLevel)
        }

        // A cancelled owner may still turn off during cleanup, until ownership transfers.
        override fun turnOff() = synchronized(guard) {
            if (active === owner) hardware.turnOff()
        }

        override fun pulseOff() = synchronized(guard) {
            if (active === owner) hardware.pulseOff()
        }
    }
}
