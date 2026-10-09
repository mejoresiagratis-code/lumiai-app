package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.repository.AccountDeletionRepository
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.PendingAccountDeletion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class DeleteAccountReport(val pending: Boolean)

/** Server-first deletion. A timeout is pending, never a successful account deletion. */
@Singleton
class DeleteAccountUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val deletion: AccountDeletionRepository,
    private val sessionData: SessionDataCleaner
) {
    private val mutex = Mutex()

    suspend fun resumePending(): Result<DeleteAccountReport>? {
        if (deletion.pending.first() == null) return null
        return invoke()
    }

    suspend operator fun invoke(): Result<DeleteAccountReport> = mutex.withLock {
        try {
            val previous = deletion.pending.first()
            val uid = previous?.uid ?: auth.currentUid()
                ?: return@withLock Result.failure(AuthException(AuthError.Unknown))
            var operation = previous ?: PendingAccountDeletion(uid, UUID.randomUUID().toString() + UUID.randomUUID().toString())
            if (!operation.serverConfirmed && previous != null) {
                try {
                    if (withTimeout(15_000L) { deletion.isCompleted(operation) }) {
                        operation = operation.copy(serverConfirmed = true)
                        deletion.savePending(operation)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                }
            }
            val current = auth.currentUid()
            if (current != null && current != uid && operation.serverConfirmed) {
                deletion.clearPending() // Completion of the old account must not touch the new session.
                return@withLock Result.success(DeleteAccountReport(pending = false))
            }
            if (current != uid && !(current == null && operation.serverConfirmed)) {
                return@withLock Result.success(DeleteAccountReport(pending = true))
            }
            if (!operation.serverConfirmed) {
                deletion.savePending(operation)
                try {
                    withTimeout(35_000L) { deletion.deleteRemotely(operation) }
                } catch (error: Exception) {
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                    if ((error as? AuthException)?.error == AuthError.RecentLoginRequired) {
                        deletion.clearPending() // Server rejected before creating a job.
                        return@withLock Result.failure(error)
                    }
                    return@withLock Result.success(DeleteAccountReport(pending = true))
                }
                deletion.savePending(operation.copy(serverConfirmed = true))
            }
            // Do not clear a different account if identity changed while the request was in flight.
            if (auth.currentUid() != null && auth.currentUid() != uid) {
                deletion.clearPending()
                return@withLock Result.success(DeleteAccountReport(pending = false))
            }
            sessionData.clearAllStrict()
            if (auth.currentUid() == uid) auth.signOut()
            deletion.clearPending()
            try { withTimeout(10_000L) { auth.ensureAnonymous() } }
            catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
            }
            Result.success(DeleteAccountReport(pending = false))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
