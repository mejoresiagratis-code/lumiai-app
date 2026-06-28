package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemePreferencesRepository {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_color")
    private val accentStyleKey = stringPreferencesKey("accent_style")

    // Default de la app: tema oscuro.
    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        runCatching { ThemeMode.valueOf(p[themeKey] ?: ThemeMode.DARK.name) }
            .getOrDefault(ThemeMode.DARK)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }

    // Default de la app: acento amarillo (linterna del Splash).
    override val accentColor: Flow<AccentColor> = dataStore.data.map { p ->
        runCatching { AccentColor.valueOf(p[accentKey] ?: AccentColor.YELLOW.name) }
            .getOrDefault(AccentColor.YELLOW)
    }

    override suspend fun setAccentColor(accent: AccentColor) {
        dataStore.edit { it[accentKey] = accent.name }
    }

    // Default de la app: estilo cálido.
    override val accentStyle: Flow<AccentStyle> = dataStore.data.map { p ->
        runCatching { AccentStyle.valueOf(p[accentStyleKey] ?: AccentStyle.WARM.name) }
            .getOrDefault(AccentStyle.WARM)
    }

    override suspend fun setAccentStyle(style: AccentStyle) {
        dataStore.edit { it[accentStyleKey] = style.name }
    }
}
