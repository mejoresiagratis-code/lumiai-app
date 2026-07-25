package com.mejoresiagratis.lumiai.data.led

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.LedBannerConfig
import com.mejoresiagratis.lumiai.domain.repository.LedBannerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreLedBannerConfigRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LedBannerConfigRepository {

    private val textKey = stringPreferencesKey("led_banner_text")
    private val argbKey = intPreferencesKey("led_banner_argb")
    private val speedKey = intPreferencesKey("led_banner_speed")
    private val leftKey = booleanPreferencesKey("led_banner_scroll_left")

    override val config: Flow<LedBannerConfig> = dataStore.data.map { p ->
        LedBannerConfig(
            text = p[textKey] ?: LedBannerConfig().text,
            argb = p[argbKey] ?: LedBannerConfig.DEFAULT_ARGB,
            speedLevel = p[speedKey] ?: LedBannerConfig.DEFAULT_SPEED,
            scrollLeft = p[leftKey] ?: true
        ).coerced()
    }

    override suspend fun setConfig(value: LedBannerConfig) {
        val c = value.coerced()
        dataStore.edit {
            it[textKey] = c.text
            it[argbKey] = c.argb
            it[speedKey] = c.speedLevel
            it[leftKey] = c.scrollLeft
        }
    }
}
