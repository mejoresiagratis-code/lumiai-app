package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingProfileTest {
    @Test fun `nombre y pais se limitan en longitud`() {
        val long = "A".repeat(200)
        val p = BillingProfile(fullName = long, billingCountry = long).coerced()
        assertEquals(BillingProfile.MAX_NAME_LEN, p.fullName.length)
        assertEquals(BillingProfile.MAX_COUNTRY_LEN, p.billingCountry.length)
    }
    @Test fun `default vacio`() {
        val p = BillingProfile()
        assertEquals("", p.fullName)
        assertEquals("", p.billingCountry)
    }
}
