package com.mejoresiagratis.lumiai.data.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Bounded foreground retries. Unacknowledged purchases are retried on the next refresh. */
internal class PurchaseAcknowledgement {
    suspend fun confirm(alreadyAcknowledged: Boolean, attempt: suspend () -> Boolean): Boolean {
        if (alreadyAcknowledged) return true
        repeat(3) { index ->
            val confirmed = try {
                attempt()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            if (confirmed) return true
            if (index < 2) delay(1_000L shl index)
        }
        return false
    }
}
