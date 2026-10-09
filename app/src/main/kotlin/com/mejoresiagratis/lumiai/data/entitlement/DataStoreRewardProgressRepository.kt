package com.mejoresiagratis.lumiai.data.entitlement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mejoresiagratis.lumiai.domain.entitlement.ProProgressReset
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRewardProgressRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val auth: com.mejoresiagratis.lumiai.domain.repository.AuthRepository
) : RewardProgressRepository {

    private val countKey = intPreferencesKey("reward_ad_count")
    private val versionKey = intPreferencesKey("reward_last_version_code")

    private val ownerKey = androidx.datastore.preferences.core.stringPreferencesKey("reward_owner_uid")
    override val count: Flow<Int> = combine(auth.currentUser, dataStore.data) { user, p ->
        if (user == null || p[ownerKey] != user.uid) 0 else (p[countKey] ?: 0).coerceIn(0, 1)
    }

    override suspend fun set(value: Int) {
        val uid = auth.currentUid() ?: return
        setForAccount(value, uid)
    }

    override suspend fun setForAccount(value: Int, uid: String): Boolean {
        var applied = false
        dataStore.edit {
            if (auth.currentUid() == uid) {
                it[countKey] = value.coerceIn(0, 1)
                it[ownerKey] = uid
                applied = true
            }
        }
        return applied
    }

    override suspend fun resetIfVersionChanged(currentVersionCode: Int) {
        dataStore.edit { prefs ->
            if (ProProgressReset.shouldReset(prefs[versionKey], currentVersionCode)) {
                prefs[countKey] = 0
            }
            prefs[versionKey] = currentVersionCode
        }
    }
}
