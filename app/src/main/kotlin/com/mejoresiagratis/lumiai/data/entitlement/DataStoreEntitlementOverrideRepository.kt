package com.mejoresiagratis.lumiai.data.entitlement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mejoresiagratis.lumiai.domain.entitlement.EntitlementOverride
import com.mejoresiagratis.lumiai.domain.repository.EntitlementOverrideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreEntitlementOverrideRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EntitlementOverrideRepository {

    private val accountKey = booleanPreferencesKey("dbg_force_account")
    private val subscriptionKey = booleanPreferencesKey("dbg_force_subscription")

    override val override: Flow<EntitlementOverride> = dataStore.data.map { p ->
        EntitlementOverride(
            forceAccount = p[accountKey],
            forceSubscription = p[subscriptionKey]
        )
    }

    override suspend fun setForceAccount(value: Boolean?) {
        dataStore.edit { if (value == null) it.remove(accountKey) else it[accountKey] = value }
    }

    override suspend fun setForceSubscription(value: Boolean?) {
        dataStore.edit { if (value == null) it.remove(subscriptionKey) else it[subscriptionKey] = value }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(accountKey)
            it.remove(subscriptionKey)
        }
    }
}
