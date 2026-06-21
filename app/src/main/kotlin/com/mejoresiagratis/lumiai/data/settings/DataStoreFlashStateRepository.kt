package com.mejoresiagratis.lumiai.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumiai_flash")

@Singleton
class DataStoreFlashStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : FlashStateRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val INTENSITY = intPreferencesKey(FlashSettingsMapper.KEY_INTENSITY)
        val STROBE = floatPreferencesKey(FlashSettingsMapper.KEY_STROBE_HZ)
        val UNIT = longPreferencesKey(FlashSettingsMapper.KEY_MORSE_UNIT)
        val ARGB = intPreferencesKey(FlashSettingsMapper.KEY_SCREEN_ARGB)
    }

    private val _isOn = MutableStateFlow(false)
    override val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    override val mode: Flow<FlashMode> = context.dataStore.data.map { p ->
        runCatching { FlashMode.valueOf(p[Keys.MODE] ?: FlashMode.CONTINUOUS.name) }
            .getOrDefault(FlashMode.CONTINUOUS)
    }

    override val settings: Flow<FlashSettings> = context.dataStore.data.map { p ->
        FlashSettingsMapper.fromMap(readMap(p))
    }

    override fun setOn(on: Boolean) {
        _isOn.value = on
    }

    override suspend fun setMode(mode: FlashMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    override suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        context.dataStore.edit { p ->
            val next = transform(FlashSettingsMapper.fromMap(readMap(p))).coerced()
            p[Keys.INTENSITY] = next.intensityLevel
            p[Keys.STROBE] = next.strobeHz
            p[Keys.UNIT] = next.morseUnitMs
            p[Keys.ARGB] = next.screenArgb
        }
    }

    private fun readMap(p: Preferences): Map<String, Any?> = buildMap {
        p[Keys.INTENSITY]?.let { put(FlashSettingsMapper.KEY_INTENSITY, it) }
        p[Keys.STROBE]?.let { put(FlashSettingsMapper.KEY_STROBE_HZ, it) }
        p[Keys.UNIT]?.let { put(FlashSettingsMapper.KEY_MORSE_UNIT, it) }
        p[Keys.ARGB]?.let { put(FlashSettingsMapper.KEY_SCREEN_ARGB, it) }
    }
}
