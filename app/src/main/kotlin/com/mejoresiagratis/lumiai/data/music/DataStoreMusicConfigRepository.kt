package com.mejoresiagratis.lumiai.data.music

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.music.MusicSensitivity
import com.mejoresiagratis.lumiai.domain.repository.MusicConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreMusicConfigRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : MusicConfigRepository {

    private val key = stringPreferencesKey("music_sensitivity")

    override val sensitivity: Flow<MusicSensitivity> = dataStore.data.map { p ->
        runCatching { MusicSensitivity.valueOf(p[key] ?: MusicSensitivity.MEDIA.name) }
            .getOrDefault(MusicSensitivity.MEDIA)
    }

    override suspend fun setSensitivity(value: MusicSensitivity) {
        dataStore.edit { it[key] = value.name }
    }
}
