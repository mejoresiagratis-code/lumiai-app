package com.mejoresiagratis.lumiai.data.sound

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertConfigRepository
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfigCodec
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSoundAlertConfigRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SoundAlertConfigRepository {

    private val key = stringPreferencesKey("sound_alert_config")

    override val config: Flow<SoundAlertConfig> =
        dataStore.data.map { SoundAlertConfigCodec.decode(it[key]) }

    override suspend fun setEnabled(category: SoundCategory, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = SoundAlertConfigCodec.decode(prefs[key])
            prefs[key] = SoundAlertConfigCodec.encode(current.withEnabled(category, enabled))
        }
    }

    override suspend fun setSensitivity(category: SoundCategory, sensitivity: Sensitivity) {
        dataStore.edit { prefs ->
            val current = SoundAlertConfigCodec.decode(prefs[key])
            prefs[key] = SoundAlertConfigCodec.encode(current.withSensitivity(category, sensitivity))
        }
    }

    override suspend fun reset() {
        dataStore.edit { it.remove(key) }
    }
}
