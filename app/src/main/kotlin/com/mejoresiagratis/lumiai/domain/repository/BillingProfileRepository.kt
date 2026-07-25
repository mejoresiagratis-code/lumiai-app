package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.BillingProfile
import kotlinx.coroutines.flow.Flow

/** Perfil de facturación persistido (nombre + país), listo para cuando se conecte Billing. */
interface BillingProfileRepository {
    val profile: Flow<BillingProfile>
    suspend fun setFullName(value: String)
    suspend fun setBillingCountry(value: String)
}
