package com.mejoresiagratis.lumiai.domain.model

/**
 * Datos de facturación del usuario. Google Play Billing procesa el cobro y emite el
 * recibo (la app nunca toca ni almacena datos de tarjeta: cumplimiento PCI por diseño).
 * Este perfil es metadato propio para futuras facturas/soporte, no un método de pago.
 */
data class BillingProfile(
    val fullName: String = "",
    val billingCountry: String = ""
) {
    fun coerced() = copy(
        fullName = fullName.take(MAX_NAME_LEN),
        billingCountry = billingCountry.take(MAX_COUNTRY_LEN)
    )

    companion object {
        const val MAX_NAME_LEN = 80
        const val MAX_COUNTRY_LEN = 56
    }
}
