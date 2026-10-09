package com.mejoresiagratis.lumiai.ui.theme

import com.mejoresiagratis.lumiai.data.profile.MemoryPreferences
import com.mejoresiagratis.lumiai.data.settings.DataStoreThemePreferencesRepository
import com.mejoresiagratis.lumiai.domain.entitlement.*
import com.mejoresiagratis.lumiai.domain.model.*
import com.mejoresiagratis.lumiai.domain.repository.*
import com.mejoresiagratis.lumiai.util.FakeFlashStateRepository
import com.mejoresiagratis.lumiai.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccentRoleTest {
    @get:Rule val main = MainDispatcherRule()
    private val user = MutableStateFlow<AuthUser?>(AuthUser("A", null, false))
    private val ent = MutableStateFlow(Entitlements(hasAccount = true, hasSubscription = true))
    private val until = MutableStateFlow(0L)
    private val auth = mockk<AuthRepository> {
        every { currentUser } returns user
        every { currentUid() } answers { user.value?.uid }
    }
    private val repo = DataStoreThemePreferencesRepository(MemoryPreferences(), auth)
    private val access = ProAccessMonitor(
        mockk<EntitlementRepository> { every { entitlements } returns ent },
        mockk<TemporaryUnlockRepository> { every { proUntilMillis } returns until }
    )

    @Test fun `subscription loss resets multicolor outside settings and does not resurrect it`() = runTest {
        repo.setAccentColor(AccentColor.MULTICOLOR)
        repo.setAccentStyle(AccentStyle.WARM)
        val vm = ThemeViewModel(repo, FakeFlashStateRepository(), access)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.accentColor.collect {} }
        runCurrent()
        assertEquals(AccentColor.MULTICOLOR, vm.accentColor.value)
        ent.value = Entitlements(hasAccount = true)
        runCurrent()
        assertEquals(AccentColor.BLUE, vm.accentColor.value)
        assertEquals(AccentStyle.VIVID, repo.accentStyle.first())
        ent.value = Entitlements(hasAccount = true, hasSubscription = true)
        runCurrent()
        assertEquals(AccentColor.BLUE, vm.accentColor.value)
    }

    @Test fun `temporary pro expiration resets multicolor but preserves free orange`() = runTest {
        ent.value = Entitlements(hasAccount = true, isEmailVerified = true)
        until.value = System.currentTimeMillis() + 60000
        repo.setAccentColor(AccentColor.MULTICOLOR)
        val vm = ThemeViewModel(repo, FakeFlashStateRepository(), access)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.accentColor.collect {} }
        runCurrent()
        assertEquals(AccentColor.MULTICOLOR, vm.accentColor.value)
        until.value = 0L
        runCurrent()
        assertEquals(AccentColor.BLUE, vm.accentColor.value)
        repo.setAccentColor(AccentColor.ORANGE)
        ent.value = Entitlements()
        runCurrent()
        assertEquals(AccentColor.ORANGE, vm.accentColor.value)
    }

    @Test fun `role matrix covers guest account unverified verified paid temporary and god`() {
        val roles = listOf(
            Triple("guest", false, false),
            Triple("unverified account", true, false),
            Triple("verified account", true, false),
            Triple("subscription", true, true),
            Triple("temporary pro", true, true),
            Triple("god", true, true)
        )
        roles.forEach { (label, account, pro) ->
            AccentColor.entries.forEach { color ->
                val expected = when (color) {
                    AccentColor.BLUE, AccentColor.ORANGE -> true
                    AccentColor.MULTICOLOR -> pro
                    else -> account || pro
                }
                assertEquals("$label / $color", expected, color.isUnlocked(account, pro))
            }
        }
    }
}
