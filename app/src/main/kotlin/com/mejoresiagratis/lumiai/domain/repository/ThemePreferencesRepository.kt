package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val accentColor: Flow<AccentColor>
    suspend fun setAccentColor(accent: AccentColor)

    val accentStyle: Flow<AccentStyle>
    suspend fun setAccentStyle(style: AccentStyle)
}
