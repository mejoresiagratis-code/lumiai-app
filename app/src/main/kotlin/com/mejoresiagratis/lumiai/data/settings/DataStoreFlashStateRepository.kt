package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreFlashStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : FlashStateRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val INTENSITY = intPreferencesKey(FlashSettingsMapper.KEY_INTENSITY)
        val STROBE = floatPreferencesKey(FlashSettingsMapper.KEY_STROBE_HZ)
        val UNIT = longPreferencesKey(FlashSettingsMapper.KEY_MORSE_UNIT)
        val ARGB = intPreferencesKey(FlashSettingsMapper.KEY_SCREEN_ARGB)
        val TEXT = stringPreferencesKey(FlashSettingsMapper.KEY_MORSE_TEXT)
        val BRIGHTNESS = floatPreferencesKey(FlashSettingsMapper.KEY_SCREEN_BRIGHTNESS)
        val BEACON_INTERVAL = longPreferencesKey(FlashSettingsMapper.KEY_BEACON_INTERVAL)
        val BEACON_FLASH = longPreferencesKey(FlashSettingsMapper.KEY_BEACON_FLASH)
        val BEACON_AUTOOFF = intPreferencesKey(FlashSettingsMapper.KEY_BEACON_AUTOOFF)
        val INTIMATE_ENABLED = booleanPreferencesKey(FlashSettingsMapper.KEY_INTIMATE_ENABLED)
        val INTIMATE_ATMOSPHERE = stringPreferencesKey(FlashSettingsMapper.KEY_INTIMATE_ATMOSPHERE)
        val INTIMATE_ANIMATION = stringPreferencesKey(FlashSettingsMapper.KEY_INTIMATE_ANIMATION)
        val INTIMATE_SLEEP_MIN = intPreferencesKey(FlashSettingsMapper.KEY_INTIMATE_SLEEP_MIN)
    }

    private val _isOn = MutableStateFlow(false)
    override val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    // Decision de producto (13-ago): la app SIEMPRE entra por Continuo cuando el proceso
    // es nuevo (app cerrada del todo o recien actualizada) — nunca hereda el ultimo modo
    // usado. In-memory (no DataStore) y sin bloquear el arranque con I/O: al ser esta clase
    // un @Singleton de Hilt, su instancia nace UNA vez por proceso — exactamente el mismo
    // ciclo de vida que "app cerrada = proceso nuevo". Reaperturas dentro del MISMO proceso
    // (minimizar/maximizar, navegar entre pantallas) siguen respetando el modo elegido.
    @Volatile private var isColdStart = true

    override val mode: Flow<FlashMode> = dataStore.data.map { p ->
        if (isColdStart) {
            isColdStart = false
            FlashMode.CONTINUOUS
        } else {
            runCatching { FlashMode.valueOf(p[Keys.MODE] ?: FlashMode.CONTINUOUS.name) }
                .getOrDefault(FlashMode.CONTINUOUS)
        }
    }

    override val settings: Flow<FlashSettings> = dataStore.data.map { p ->
        FlashSettingsMapper.fromMap(readMap(p))
    }

    override fun setOn(on: Boolean) {
        _isOn.value = on
    }

    override suspend fun setMode(mode: FlashMode) {
        dataStore.edit { it[Keys.MODE] = mode.name }
    }

    override suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        dataStore.edit { p ->
            val next = transform(FlashSettingsMapper.fromMap(readMap(p))).coerced()
            p[Keys.INTENSITY] = next.intensityLevel
            p[Keys.STROBE] = next.strobeHz
            p[Keys.UNIT] = next.morseUnitMs
            p[Keys.ARGB] = next.screenArgb
            p[Keys.TEXT] = next.morseText
            p[Keys.BRIGHTNESS] = next.screenBrightness
            p[Keys.BEACON_INTERVAL] = next.beaconIntervalMs
            p[Keys.BEACON_FLASH] = next.beaconFlashMs
            p[Keys.BEACON_AUTOOFF] = next.beaconAutoOffMin
            p[Keys.INTIMATE_ENABLED] = next.intimateEnabled
            p[Keys.INTIMATE_ATMOSPHERE] = next.intimateAtmosphere.name
            p[Keys.INTIMATE_ANIMATION] = next.intimateAnimation.name
            p[Keys.INTIMATE_SLEEP_MIN] = next.intimateSleepMinutes
        }
    }

    private fun readMap(p: Preferences): Map<String, Any?> = buildMap {
        p[Keys.INTENSITY]?.let { put(FlashSettingsMapper.KEY_INTENSITY, it) }
        p[Keys.STROBE]?.let { put(FlashSettingsMapper.KEY_STROBE_HZ, it) }
        p[Keys.UNIT]?.let { put(FlashSettingsMapper.KEY_MORSE_UNIT, it) }
        p[Keys.ARGB]?.let { put(FlashSettingsMapper.KEY_SCREEN_ARGB, it) }
        p[Keys.TEXT]?.let { put(FlashSettingsMapper.KEY_MORSE_TEXT, it) }
        p[Keys.BRIGHTNESS]?.let { put(FlashSettingsMapper.KEY_SCREEN_BRIGHTNESS, it) }
        p[Keys.BEACON_INTERVAL]?.let { put(FlashSettingsMapper.KEY_BEACON_INTERVAL, it) }
        p[Keys.BEACON_FLASH]?.let { put(FlashSettingsMapper.KEY_BEACON_FLASH, it) }
        p[Keys.BEACON_AUTOOFF]?.let { put(FlashSettingsMapper.KEY_BEACON_AUTOOFF, it) }
        p[Keys.INTIMATE_ENABLED]?.let { put(FlashSettingsMapper.KEY_INTIMATE_ENABLED, it) }
        p[Keys.INTIMATE_ATMOSPHERE]?.let { put(FlashSettingsMapper.KEY_INTIMATE_ATMOSPHERE, it) }
        p[Keys.INTIMATE_ANIMATION]?.let { put(FlashSettingsMapper.KEY_INTIMATE_ANIMATION, it) }
        p[Keys.INTIMATE_SLEEP_MIN]?.let { put(FlashSettingsMapper.KEY_INTIMATE_SLEEP_MIN, it) }
    }
}
