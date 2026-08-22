package com.mejoresiagratis.lumiai.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.BillingProfile
import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreBillingProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : BillingProfileRepository {

    private val nameKey = stringPreferencesKey("billing_full_name")
    private val countryKey = stringPreferencesKey("billing_country")

    override val profile: Flow<BillingProfile> = dataStore.data.map { p ->
        BillingProfile(
            fullName = p[nameKey] ?: "",
            billingCountry = p[countryKey] ?: ""
        )
    }

    override suspend fun setFullName(value: String) {
        dataStore.edit { it[nameKey] = value.take(BillingProfile.MAX_NAME_LEN) }
    }

    override suspend fun setBillingCountry(value: String) {
        dataStore.edit { it[countryKey] = value.take(BillingProfile.MAX_COUNTRY_LEN) }
    }

    override suspend fun prefillFullNameIfEmpty(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            if (prefs[nameKey].isNullOrBlank()) {
                prefs[nameKey] = trimmed.take(BillingProfile.MAX_NAME_LEN)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(nameKey)
            prefs.remove(countryKey)
        }
    }

    override suspend fun prefillCountryIfEmpty(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            if (prefs[countryKey].isNullOrBlank()) {
                prefs[countryKey] = trimmed.take(BillingProfile.MAX_COUNTRY_LEN)
            }
        }
    }
}
