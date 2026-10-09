package com.mejoresiagratis.lumiai.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.mejoresiagratis.lumiai.data.entitlement.DataStoreRewardProgressRepository
import com.mejoresiagratis.lumiai.data.entitlement.DataStoreTemporaryUnlockRepository
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MemoryPreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    var beforeUpdate: () -> Unit = {}
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        beforeUpdate()
        return transform(data.value).also { data.value = it }
    }
}

class AccountOwnedPreferencesTest {
    private val store = MemoryPreferences()
    private val users = MutableStateFlow<AuthUser?>(AuthUser("A", "a@example.test", false, true))
    private val auth = mockk<AuthRepository> {
        every { currentUser } returns users
        every { currentUid() } answers { users.value?.uid }
    }
    private val profile = DataStoreBillingProfileRepository(store, auth)
    private val rewards = DataStoreRewardProgressRepository(store, auth)
    private val unlock = DataStoreTemporaryUnlockRepository(auth)
    private fun switch() { users.value = AuthUser("B", "b@example.test", false, true) }

    @Test fun `new account never receives previous profile or rewards`() = runTest {
        profile.setFullName("Account A")
        profile.setBillingCountry("Spain")
        rewards.set(1)
        unlock.extend(60000)
        switch()
        assertEquals("", profile.profile.first().fullName)
        assertNull(profile.profile.first().ownerUid)
        assertEquals(0, rewards.count.first())
        assertEquals(0L, unlock.proUntilMillis.first())
        profile.setBillingCountry("France")
        assertEquals("", profile.profile.first().fullName)
        assertEquals("France", profile.profile.first().billingCountry)
    }

    @Test fun `legacy unowned personal data is not assigned to current account`() = runTest {
        store.edit { it[stringPreferencesKey("billing_full_name")] = "Unknown owner" }
        assertEquals("", profile.profile.first().fullName)
        profile.prefillFullNameIfEmpty("Current user")
        assertEquals("Current user", profile.profile.first().fullName)
    }

    @Test fun `queued profile write is discarded after identity changes`() = runTest {
        store.beforeUpdate = { switch() }
        profile.setFullName("Old account edit")
        assertEquals("", profile.profile.first().fullName)
    }

    @Test fun `late reward cannot credit new account`() = runTest {
        switch()
        assertFalse(rewards.setForAccount(1, "A"))
        assertFalse(unlock.extendForAccount(60000, "A"))
        assertEquals(0, rewards.count.first())
        assertEquals(0L, unlock.proUntilMillis.first())
    }

    @Test fun `reward checks identity inside datastore transaction`() = runTest {
        store.beforeUpdate = { switch() }
        assertFalse(rewards.setForAccount(1, "A"))
        assertEquals(0, rewards.count.first())
    }

    @Test fun `prefill does not overwrite explicit profile values`() = runTest {
        profile.setFullName("Edited")
        profile.prefillFullNameIfEmpty("Provider")
        assertEquals("Edited", profile.profile.first().fullName)
    }
}
