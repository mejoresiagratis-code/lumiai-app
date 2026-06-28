package com.mejoresiagratis.lumiai.data.entitlement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRewardProgressRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : RewardProgressRepository {

    private val countKey = intPreferencesKey("reward_ad_count")

    override val count: Flow<Int> = dataStore.data.map { it[countKey] ?: 0 }

    override suspend fun set(value: Int) {
        dataStore.edit { it[countKey] = value.coerceAtLeast(0) }
    }
}
