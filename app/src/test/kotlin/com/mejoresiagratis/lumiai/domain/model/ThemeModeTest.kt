package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `next cycles through all modes and wraps`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }
}
