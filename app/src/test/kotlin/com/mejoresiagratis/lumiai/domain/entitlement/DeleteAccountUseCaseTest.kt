package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteAccountUseCaseTest {
    private val events = mutableListOf<String>()
    private var uid: String? = "owner"
    private val auth = mockk<AuthRepository>(relaxed = true) {
        every { currentUid() } answers { uid }
        coEvery { signOut() } coAnswers { events += "signOut"; uid = null }
    }
    private val cleaner = mockk<SessionDataCleaner> {
        coEvery { clearAllStrict() } coAnswers { events += "clean" }
    }
    private val deletion = object : AccountDeletionRepository {
        override val pending = MutableStateFlow<PendingAccountDeletion?>(null)
        var remote: suspend () -> Unit = { events += "remote" }
        var completed = false
        override suspend fun savePending(value: PendingAccountDeletion) { pending.value = value }
        override suspend fun clearPending() { pending.value = null }
        override suspend fun deleteRemotely(pending: PendingAccountDeletion) { remote() }
        override suspend fun isCompleted(pending: PendingAccountDeletion) = completed
    }
    private fun useCase() = DeleteAccountUseCase(auth, deletion, cleaner)

    @Test fun successCleansOnlyAfterServerConfirmation() = runTest {
        assertFalse(useCase()().getOrThrow().pending)
        assertEquals(listOf("remote", "clean", "signOut"), events)
        assertNull(deletion.pending.value)
    }

    @Test fun networkFailureKeepsAccountAndLocalData() = runTest {
        deletion.remote = { error("offline") }
        assertTrue(useCase()().getOrThrow().pending)
        assertEquals("owner", uid)
        assertNotNull(deletion.pending.value)
        coVerify(exactly = 0) { cleaner.clearAllStrict() }
    }

    @Test fun recentLoginRejectionDoesNotEraseAnyData() = runTest {
        deletion.remote = { throw AuthException(AuthError.RecentLoginRequired) }
        val result = useCase()()
        assertEquals(AuthError.RecentLoginRequired, (result.exceptionOrNull() as AuthException).error)
        assertNull(deletion.pending.value)
        assertTrue(events.isEmpty())
    }

    @Test fun lostResponseCanBeConfirmedAfterRestartWithoutAuth() = runTest {
        deletion.pending.value = PendingAccountDeletion("owner", "receipt")
        deletion.completed = true
        uid = null
        assertFalse(useCase().resumePending()!!.getOrThrow().pending)
        assertEquals(listOf("clean"), events)
        assertNull(deletion.pending.value)
    }

    @Test fun differentAccountIsNeverClearedOrDeleted() = runTest {
        deletion.pending.value = PendingAccountDeletion("owner", "receipt", true)
        uid = "someone-else"
        assertTrue(useCase().resumePending()!!.getOrThrow().pending)
        assertTrue(events.isEmpty())
        assertNotNull(deletion.pending.value)
    }

    @Test fun coroutineCancellationPropagatesAndRetainsReceipt() = runTest {
        deletion.remote = { throw CancellationException("screen closed") }
        try { useCase()(); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        assertNotNull(deletion.pending.value)
        assertTrue(events.isEmpty())
    }

    @Test fun timeoutReportsPendingNotSuccessAndRetainsData() = runTest {
        deletion.remote = { delay(40_000) }
        assertTrue(useCase()().getOrThrow().pending)
        assertNotNull(deletion.pending.value)
        assertTrue(events.isEmpty())
    }

    @Test fun failedLocalCleanupRetriesWithoutDeletingAgain() = runTest {
        coEvery { cleaner.clearAllStrict() } throws IllegalStateException("disk")
        assertTrue(useCase()().isFailure)
        assertTrue(deletion.pending.value!!.serverConfirmed)
        coEvery { cleaner.clearAllStrict() } coAnswers { events += "clean" }
        assertFalse(useCase().resumePending()!!.getOrThrow().pending)
        assertEquals(listOf("remote", "clean", "signOut"), events)
    }
}
