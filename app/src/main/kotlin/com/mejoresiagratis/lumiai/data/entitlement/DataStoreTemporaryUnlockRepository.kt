package com.mejoresiagratis.lumiai.data.entitlement

import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Desbloqueo temporal de Pro — **estado de sesión puro, EN MEMORIA** (corregido 17-ago).
 *
 * ## Por qué ya no se persiste
 * La regla de producto es que la hora de Pro **muere al cerrar la app**. La implementación
 * anterior sí persistía en DataStore y trataba de simular esa muerte con un flag
 * `isColdStart` que devolvía 0 en la primera emisión. **Era incorrecto:** `proUntilMillis`
 * tiene CUATRO colectores (FlashViewModel, RewardedUnlockViewModel, GodViewModel y
 * RecordRewardUseCase). El primero consumía el flag y recibía 0, pero los otros tres leían
 * el valor persistido real — la hora de Pro RESUCITABA tras cerrar la app, y `extend()`
 * además prolongaba esa fecha vieja.
 *
 * El arreglo correcto no es parchear el flag, es no persistir: si el dato debe morir con el
 * proceso, su sitio es la memoria. Al ser este un `@Singleton` de Hilt, la instancia nace una
 * vez por proceso, así que "proceso nuevo" equivale exactamente a "sin desbloqueo". Sin
 * carreras, sin lecturas obsoletas y sin simular nada.
 */
@Singleton
class DataStoreTemporaryUnlockRepository @Inject constructor(
    private val auth: com.mejoresiagratis.lumiai.domain.repository.AuthRepository
) : TemporaryUnlockRepository {
    private data class Grant(val uid: String? = null, val until: Long = 0L)
    private val grant = MutableStateFlow(Grant())
    override val proUntilMillis: Flow<Long> = combine(auth.currentUser, grant) { user, current ->
        if (user?.uid == current.uid) current.until else 0L
    }

    override suspend fun extend(durationMillis: Long) {
        val uid = auth.currentUid() ?: return
        extendForAccount(durationMillis, uid)
    }

    override suspend fun extendForAccount(durationMillis: Long, uid: String): Boolean {
        if (auth.currentUid() != uid) return false
        val old = grant.value
        grant.value = Grant(uid, TemporaryUnlock.extended(
            currentUntilMillis = if (old.uid == uid) old.until else 0L,
            nowMillis = System.currentTimeMillis(), durationMillis = durationMillis
        ))
        return true
    }

    override suspend fun clear() { grant.value = Grant() }
}
