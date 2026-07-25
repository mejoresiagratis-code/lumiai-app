package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.BillingProfile
import kotlinx.coroutines.flow.Flow

/** Perfil de facturación persistido (nombre + país), listo para cuando se conecte Billing. */
interface BillingProfileRepository {
    val profile: Flow<BillingProfile>
    suspend fun setFullName(value: String)
    suspend fun setBillingCountry(value: String)

    /**
     * Rellena el nombre completo con [value] SOLO si el campo está vacío. Se usa para sembrar el
     * perfil con el nombre del proveedor (p. ej. Google) tras iniciar sesión, sin pisar nunca lo
     * que el usuario haya escrito a mano. Idempotente y atómico dentro de DataStore.
     */
    suspend fun prefillFullNameIfEmpty(value: String)

    /** País de facturación sembrado por defecto SOLO si está vacío (p. ej. del Locale). */
    suspend fun prefillCountryIfEmpty(value: String)
}
