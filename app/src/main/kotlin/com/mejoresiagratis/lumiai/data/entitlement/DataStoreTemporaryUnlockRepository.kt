package com.mejoresiagratis.lumiai.data.entitlement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreTemporaryUnlockRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : TemporaryUnlockRepository {

    private val proUntilKey = longPreferencesKey("pro_until_millis")

    override val proUntilMillis: Flow<Long> = dataStore.data.map { it[proUntilKey] ?: 0L }

    override suspend fun extend(durationMillis: Long) {
        dataStore.edit { prefs ->
            val current = prefs[proUntilKey] ?: 0L
            prefs[proUntilKey] = TemporaryUnlock.extended(current, System.currentTimeMillis(), durationMillis)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(proUntilKey) }
    }
}
