package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso Pro EFECTIVO en tiempo real: permisos permanentes + desbloqueo temporal por anuncios,
 * reevaluado cada segundo para detectar el instante de caducidad (22-ago).
 *
 * ## Por qué existe
 * Esta combinación vivía solo dentro de `FlashViewModel`, es decir, **solo en la interfaz**.
 * Consecuencia real detectada en auditoría: si caducaba la hora de Pro, se cerraba sesión o se
 * borraba la cuenta **mientras Música o Alerta sonora estaban corriendo**, los servicios seguían
 * usando micrófono y flash indefinidamente. Ocultar un botón no revoca una ejecución en curso.
 *
 * Al ser un `@Singleton` compartido, servicios y pantallas evalúan el acceso con la MISMA regla:
 * si algún día cambia, cambia para todos a la vez y no puede haber discrepancias entre lo que
 * la interfaz muestra y lo que los servicios permiten.
 */
@Singleton
class ProAccessMonitor @Inject constructor(
    entitlementRepo: EntitlementRepository,
    temporaryUnlock: TemporaryUnlockRepository
) {

    /** Reloj de 1 s: sin él, la caducidad del Pro temporal no se detectaría hasta el siguiente
     *  cambio de permisos. Es un flujo frío: solo late mientras alguien lo observa. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MS)
        }
    }

    val access: Flow<AccessState> = combine(
        entitlementRepo.entitlements,
        temporaryUnlock.proUntilMillis,
        ticker
    ) { ent, proUntil, now ->
        AccessState(
            entitlements = ent,
            temporaryProActive = TemporaryUnlock.isActive(proUntil, now)
        )
    }

    /**
     * ¿Hay acceso a las funciones de tier IA (Música, Alerta sonora, Letrero LED)?
     *
     * `distinctUntilChanged` es OBLIGATORIO, no cosmético: el ticker emite cada segundo, y un
     * servicio que reaccionara a cada emisión se pararía y arrancaría en bucle.
     */
    val hasAiAccess: Flow<Boolean> =
        access.map { it.unlocks(Tier.AI) }.distinctUntilChanged()

    private companion object {
        const val TICK_MS = 1_000L
    }
}
