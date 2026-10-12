package com.mejoresiagratis.lumiai.data.entitlement

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Desbloqueo temporal de Pro persistido en DataStore y asociado a la cuenta que vio los anuncios.
 *
 * El instante de caducidad sobrevive al cierre del proceso. La concesión solo se expone mientras
 * la cuenta autenticada coincide con la propietaria; una hora ganada nunca pasa a otra cuenta.
 */
@Singleton
class DataStoreTemporaryUnlockRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val auth: AuthRepository
) : TemporaryUnlockRepository {

    private val untilKey = longPreferencesKey("temporary_pro_until_millis")
    private val ownerKey = stringPreferencesKey("temporary_pro_owner_uid")

    override val proUntilMillis: Flow<Long> = combine(auth.currentUser, dataStore.data) { user, prefs ->
        val uid = user?.uid ?: return@combine 0L
        if (prefs[ownerKey] == uid) prefs[untilKey] ?: 0L else 0L
    }

    override suspend fun extend(durationMillis: Long) {
        if (durationMillis <= 0L) return
        val uid = auth.currentUid() ?: return
        extendForAccount(durationMillis, uid)
    }

    override suspend fun extendForAccount(durationMillis: Long, uid: String): Boolean {
        if (durationMillis <= 0L || auth.currentUid() != uid) return false
        var applied = false
        dataStore.edit { prefs ->
            // Recheck the owner inside the transaction so a late ad callback cannot grant Pro
            // after the user has switched accounts.
            if (auth.currentUid() == uid) {
                val previousUntil = if (prefs[ownerKey] == uid) prefs[untilKey] ?: 0L else 0L
                prefs[ownerKey] = uid
                prefs[untilKey] = TemporaryUnlock.extended(
                    currentUntilMillis = previousUntil,
                    nowMillis = System.currentTimeMillis(),
                    durationMillis = durationMillis
                )
                applied = true
            }
        }
        return applied
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(untilKey)
            prefs.remove(ownerKey)
        }
    }
}
