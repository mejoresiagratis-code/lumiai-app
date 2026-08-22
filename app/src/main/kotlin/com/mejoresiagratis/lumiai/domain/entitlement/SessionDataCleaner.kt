package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Borra TODO el estado ligado a una cuenta concreta (17-ago).
 *
 * ## El defecto que corrige
 * El nombre y el país de facturación se guardaban en claves globales, sin UID, y solo se
 * limpiaba el desbloqueo temporal al cerrar sesión. Consecuencia real: si una segunda persona
 * iniciaba sesión en el mismo móvil, **heredaba los datos personales de la primera** — y el
 * sincronizador los subía a Firestore **bajo el UID nuevo**. El contador de anuncios tenía el
 * mismo problema: la cuenta B empezaba con los anuncios vistos por A.
 *
 * ## Por qué limpiar y no separar por UID
 * La alternativa era guardar cada clave con el UID dentro (`billing_full_name:$uid`). Es más
 * "puro" y permitiría varias cuentas en el mismo dispositivo sin pisarse, pero obliga a migrar
 * las instalaciones existentes y multiplica las claves. LumiAI es de un perfil por móvil: aquí
 * limpiar al cambiar de dueño es más simple, verificable de un vistazo y sin migración.
 *
 * Un único punto de entrada a propósito: cada vez que este borrado se ha hecho "a mano" en el
 * sitio que tocaba, se ha olvidado alguna pieza.
 */
@Singleton
class SessionDataCleaner @Inject constructor(
    private val billingProfile: BillingProfileRepository,
    private val rewardProgress: RewardProgressRepository,
    private val temporaryUnlock: TemporaryUnlockRepository
) {

    /**
     * Limpia el estado de la sesión anterior. Cada paso va por separado: si uno falla, los
     * demás se ejecutan igualmente — dejar datos personales de otra persona sería peor que
     * un fallo parcial silencioso.
     */
    suspend fun clearAll() {
        runCatching { billingProfile.clear() }
        runCatching { rewardProgress.set(0) }
        runCatching { temporaryUnlock.clear() }
    }
}
