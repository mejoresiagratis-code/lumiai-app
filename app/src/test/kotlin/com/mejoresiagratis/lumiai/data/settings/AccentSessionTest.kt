package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.data.profile.MemoryPreferences
import com.mejoresiagratis.lumiai.domain.model.*
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccentSessionTest {
    private val store = MemoryPreferences()
    private val users = MutableStateFlow<AuthUser?>(AuthUser("A", null, false))
    private val auth = mockk<AuthRepository> {
        every { currentUser } returns users
        every { currentUid() } answers { users.value?.uid }
    }
    private val theme = DataStoreThemePreferencesRepository(store, auth)

    @Test fun `logout resets every color and style and preserves dark mode`() = runTest {
        theme.setThemeMode(ThemeMode.DARK)
        for (color in AccentColor.entries) {
            users.value = AuthUser("A", null, false)
            theme.setAccentColor(color)
            theme.setAccentStyle(AccentStyle.WARM)
            theme.resetAccent()
            users.value = null
            assertEquals(color.name, AccentColor.BLUE, theme.accentColor.first())
            assertEquals(AccentStyle.VIVID, theme.accentStyle.first())
            users.value = AuthUser("A", null, false)
            assertEquals(AccentColor.BLUE, theme.accentColor.first())
        }
        assertEquals(ThemeMode.DARK, theme.themeMode.first())
    }

    @Test fun `direct account switch does not inherit free orange or warm style`() = runTest {
        theme.setAccentColor(AccentColor.ORANGE)
        theme.setAccentStyle(AccentStyle.WARM)
        users.value = AuthUser("B", null, false)
        assertEquals(AccentColor.BLUE, theme.accentColor.first())
        assertEquals(AccentStyle.VIVID, theme.accentStyle.first())
    }

    @Test fun `linking anonymous user resets accent even when uid stays the same`() = runTest {
        users.value = AuthUser("A", null, true)
        theme.setAccentColor(AccentColor.ORANGE)
        users.value = AuthUser("A", null, false)
        assertEquals(AccentColor.BLUE, theme.accentColor.first())
    }

    @Test fun `same user retains chosen color across repository recreation`() = runTest {
        theme.setAccentColor(AccentColor.ORANGE)
        theme.setAccentStyle(AccentStyle.WARM)
        val reopened = DataStoreThemePreferencesRepository(store, auth)
        assertEquals(AccentColor.ORANGE, reopened.accentColor.first())
        assertEquals(AccentStyle.WARM, reopened.accentStyle.first())
    }

    @Test fun `delayed write cannot apply to another account`() = runTest {
        store.beforeUpdate = { users.value = AuthUser("B", null, false) }
        theme.setAccentColor(AccentColor.ORANGE)
        assertEquals(AccentColor.BLUE, theme.accentColor.first())
    }

    @Test fun `stale blocked-color reset preserves newer selection`() = runTest {
        theme.setAccentColor(AccentColor.ORANGE)
        theme.resetAccentIfMatches(AccentColor.MULTICOLOR)
        assertEquals(AccentColor.ORANGE, theme.accentColor.first())
    }
}
