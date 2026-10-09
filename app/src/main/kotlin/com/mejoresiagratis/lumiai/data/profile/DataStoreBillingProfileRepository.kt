package com.mejoresiagratis.lumiai.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.BillingProfile
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreBillingProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val auth: AuthRepository
) : BillingProfileRepository {
    private val nameKey = stringPreferencesKey("billing_full_name")
    private val countryKey = stringPreferencesKey("billing_country")
    private val ownerKey = stringPreferencesKey("billing_owner_uid")

    // Unowned legacy fields are not guessed to belong to whichever user signs in next.
    override val profile: Flow<BillingProfile> = combine(auth.currentUser, dataStore.data) { user, p ->
        val uid = user?.takeUnless { it.isAnonymous }?.uid
        if (uid == null || p[ownerKey] != uid) BillingProfile()
        else BillingProfile(p[nameKey] ?: "", p[countryKey] ?: "", uid)
    }

    private suspend fun editOwned(block: (MutablePreferences) -> Unit) {
        val uid = auth.currentUid() ?: return
        dataStore.edit { p ->
            if (auth.currentUid() != uid) return@edit
            if (p[ownerKey] != uid) {
                p.remove(nameKey)
                p.remove(countryKey)
                p[ownerKey] = uid
            }
            block(p)
        }
    }

    override suspend fun setFullName(value: String) = editOwned {
        it[nameKey] = value.take(BillingProfile.MAX_NAME_LEN)
    }
    override suspend fun setBillingCountry(value: String) = editOwned {
        it[countryKey] = value.take(BillingProfile.MAX_COUNTRY_LEN)
    }
    override suspend fun prefillFullNameIfEmpty(value: String) = editOwned {
        if (it[nameKey].isNullOrBlank() && value.isNotBlank())
            it[nameKey] = value.trim().take(BillingProfile.MAX_NAME_LEN)
    }
    override suspend fun prefillCountryIfEmpty(value: String) = editOwned {
        if (it[countryKey].isNullOrBlank() && value.isNotBlank())
            it[countryKey] = value.trim().take(BillingProfile.MAX_COUNTRY_LEN)
    }
    override suspend fun clear() {
        val uid = auth.currentUid()
        dataStore.edit { p ->
            if (auth.currentUid() != uid) return@edit
            p.remove(nameKey)
            p.remove(countryKey)
            p.remove(ownerKey)
        }
    }
}
