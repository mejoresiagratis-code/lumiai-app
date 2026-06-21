package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private val key = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        runCatching { ThemeMode.valueOf(p[key] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[key] = mode.name }
    }
}
