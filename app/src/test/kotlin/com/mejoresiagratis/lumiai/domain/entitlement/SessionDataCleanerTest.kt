package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SessionDataCleanerTest {
    private val profile = mockk<BillingProfileRepository>(relaxed = true)
    private val progress = mockk<RewardProgressRepository>(relaxed = true)
    private val unlock = mockk<TemporaryUnlockRepository>(relaxed = true)
    private val theme = mockk<ThemePreferencesRepository>(relaxed = true)
    private val cleaner = SessionDataCleaner(profile, progress, unlock, theme)

    @Test fun `write failure is reported after other cleanup is attempted`() = runTest {
        coEvery { profile.clear() } throws java.io.IOException("disk")
        var failed = false
        try { cleaner.clearAll() } catch (_: java.io.IOException) { failed = true }
        assertTrue(failed)
        coVerify { progress.set(0); unlock.clear(); theme.resetAccent(); theme.resetGuestTheme() }
    }

    @Test fun `cancellation is propagated without starting new cleanup`() = runTest {
        coEvery { profile.clear() } throws CancellationException()
        var cancelled = false
        try { cleaner.clearAll() } catch (_: CancellationException) { cancelled = true }
        assertTrue(cancelled)
        coVerify(exactly = 0) { progress.set(any()); unlock.clear() }
    }
}
