package com.mejoresiagratis.lumiai.data.entitlement

import com.mejoresiagratis.lumiai.BuildConfig
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementOverrideRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEntitlementRepository @Inject constructor(
    auth: AuthRepository,
    overrideRepo: EntitlementOverrideRepository,
    subscriptionRepo: SubscriptionRepository
) : EntitlementRepository {
    // Permisos reales: cuenta desde Firebase Auth, suscripcion desde Play Billing (verificada
    // contra Google Play, no un campo editable por el cliente). En builds debug, el override de
    // superusuario (God) puede forzarlos; en release el override se ignora.
    override val entitlements: Flow<Entitlements> =
        combine(auth.currentUser, overrideRepo.override, subscriptionRepo.isSubscribed) { user, ov, subscribed ->
            val base = Entitlements(
                hasAccount = user != null && !user.isAnonymous,
                hasSubscription = subscribed,
                isEmailVerified = user?.isEmailVerified == true
            )
            if (BuildConfig.DEBUG) ov.apply(base) else base
        }
}
