package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreOnboardingPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : OnboardingPreferencesRepository {

    private val key = booleanPreferencesKey("onboarding_completed")

    override val completed: Flow<Boolean> = dataStore.data.map { it[key] ?: false }

    override suspend fun setCompleted() {
        dataStore.edit { it[key] = true }
    }
}
