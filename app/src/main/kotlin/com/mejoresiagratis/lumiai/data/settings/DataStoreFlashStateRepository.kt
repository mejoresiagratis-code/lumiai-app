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

    // Decision de producto (13-ago): la app SIEMPRE entra por Continuo con proceso nuevo.
    //
    // CORREGIDO 17-ago: antes esto se simulaba con un flag `isColdStart` sobre el flujo
    // persistido, devolviendo CONTINUOUS en la primera emision. Era INCORRECTO — `mode`
    // tiene DOS colectores (FlashViewModel y TorchService): el primero consumia el flag y
    // recibia CONTINUOUS, pero el segundo leia el modo persistido real. El arranque en frio
    // no estaba garantizado en absoluto.
    //
    // El modo es estado de SESION, no preferencia persistente: su sitio es la memoria. Como
    // esta clase es @Singleton de Hilt, la instancia nace una vez por proceso — "proceso
    // nuevo" es exactamente "vuelta a Continuo". Los ajustes (settings) SI se persisten:
    // esos deben sobrevivir entre sesiones. La clave MODE de DataStore queda en desuso.
    private val _mode = MutableStateFlow(FlashMode.CONTINUOUS)
    override val mode: Flow<FlashMode> = _mode.asStateFlow()

    override val settings: Flow<FlashSettings> = dataStore.data.map { p ->
        FlashSettingsMapper.fromMap(readMap(p))
    }

    override fun setOn(on: Boolean) {
        _isOn.value = on
    }

    override suspend fun setMode(mode: FlashMode) {
        _mode.value = mode
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
