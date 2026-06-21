package com.mejoresiagratis.lumiai.data.entitlement

import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEntitlementRepository @Inject constructor(
    auth: AuthRepository
) : EntitlementRepository {
    // Fase 4/5 combinarán aquí desbloqueos temporales (ads) y suscripción.
    override val entitlements: Flow<Entitlements> = auth.currentUser.map { user ->
        Entitlements(
            hasAccount = user != null && !user.isAnonymous,
            hasSubscription = false
        )
    }
}
