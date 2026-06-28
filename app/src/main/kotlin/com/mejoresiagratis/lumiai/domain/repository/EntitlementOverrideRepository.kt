package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.entitlement.EntitlementOverride
import kotlinx.coroutines.flow.Flow

/** Persiste el override de superusuario (solo debug). null en un campo = no forzar. */
interface EntitlementOverrideRepository {
    val override: Flow<EntitlementOverride>
    suspend fun setForceAccount(value: Boolean?)
    suspend fun setForceSubscription(value: Boolean?)
    suspend fun clear()
}
