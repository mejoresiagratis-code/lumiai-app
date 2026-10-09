package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

data class PendingAccountDeletion(val uid: String, val receipt: String, val serverConfirmed: Boolean = false)

interface AccountDeletionRepository {
    val pending: Flow<PendingAccountDeletion?>
    suspend fun savePending(value: PendingAccountDeletion)
    suspend fun clearPending()
    /** Completes only after the server confirms registry and Auth deletion. */
    suspend fun deleteRemotely(pending: PendingAccountDeletion)
    suspend fun isCompleted(pending: PendingAccountDeletion): Boolean
}
