package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
