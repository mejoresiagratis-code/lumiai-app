package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {
    val entitlements: Flow<Entitlements>
}
