package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.data.profile.MemoryPreferences
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountThemeTest {
    private val store = MemoryPreferences()
    private val users = MutableStateFlow<AuthUser?>(AuthUser("A", null, false))
    private val auth = mockk<AuthRepository> {
        every { currentUser } returns users
        every { currentUid() } answers { users.value?.uid }
    }
    private val repo = DataStoreThemePreferencesRepository(store, auth)

    @Test fun `accounts retain independent choices after switching and reopening`() = runTest {
        repo.setThemeMode(ThemeMode.DARK)
        users.value = AuthUser("B", null, false)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        repo.setThemeMode(ThemeMode.LIGHT)
        users.value = AuthUser("A", null, false)
        assertEquals(ThemeMode.DARK, DataStoreThemePreferencesRepository(store, auth).themeMode.first())
        users.value = AuthUser("B", null, false)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())
    }

    @Test fun `logout resets guest without erasing account preference`() = runTest {
        users.value = null
        repo.setThemeMode(ThemeMode.DARK)
        users.value = AuthUser("A", null, false)
        repo.setThemeMode(ThemeMode.LIGHT)
        repo.resetGuestTheme()
        users.value = null
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        users.value = AuthUser("A", null, false)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())
    }

    @Test fun `legacy unowned dark preference is not inherited`() = runTest {
        store.edit { it[stringPreferencesKey("theme_mode")] = ThemeMode.DARK.name }
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        users.value = null
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test fun `anonymous identity does not seed linked account`() = runTest {
        users.value = AuthUser("A", null, true)
        repo.setThemeMode(ThemeMode.DARK)
        users.value = AuthUser("A", null, false)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test fun `queued write after account change is discarded`() = runTest {
        store.beforeUpdate = { users.value = AuthUser("B", null, false) }
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        users.value = AuthUser("A", null, false)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test fun `queued guest write cannot leak into newly linked same uid`() = runTest {
        users.value = AuthUser("A", null, true)
        store.beforeUpdate = { users.value = AuthUser("A", null, false) }
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        users.value = null
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test fun `system choice replaces explicit mode and survives reopening`() = runTest {
        repo.setThemeMode(ThemeMode.DARK)
        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, DataStoreThemePreferencesRepository(store, auth).themeMode.first())
    }

    @Test fun `invalid stored mode falls back to system`() = runTest {
        store.edit { it[stringPreferencesKey("theme_mode_v2:account:A")] = "INVALID" }
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }
}
