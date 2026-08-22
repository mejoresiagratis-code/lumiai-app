package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/** Qué se consiguió borrar realmente. Se muestra al usuario si algo quedó pendiente. */
data class DeleteAccountReport(
    val registryDeleted: Boolean,
    val registryFailure: String? = null
)

/**
 * Borrado de cuenta completo, con tiempos límite y sin dejar al usuario colgado.
 *
 * ## Regresión que corrige — QA de Pablo, 22-ago
 * La primera versión de este caso de uso, escrita ayer, dejaba la app **congelada sin ningún
 * aviso**: se pulsaba «Borrar cuenta» y no ocurría nada, ni error ni diálogo. Dos causas, ambas
 * mías:
 *
 * 1. **Espera sin límite.** `userRegistry.delete()` aguarda la confirmación del SERVIDOR de
 *    Firestore. Con la persistencia offline —activa por defecto en Android— o con App Check
 *    rechazando la petición, esa tarea puede no completarse jamás. La corrutina se quedaba
 *    esperando para siempre y el punto de no retorno nunca llegaba.
 * 2. **Abortar por el espejo.** Decidí que un fallo de Firestore cancelara todo el borrado.
 *    Era un error de criterio: el registro es un ESPEJO administrativo, y dejar que un problema
 *    de red impida a alguien ejercer su derecho de supresión es peor que conservar un documento
 *    residual que se puede purgar desde la consola.
 *
 * ## Comportamiento actual
 * Cada paso de red tiene tiempo límite. El espejo se intenta primero —mientras las credenciales
 * aún existen, porque las reglas exigen ser el dueño— pero **su fallo ya no bloquea nada**: se
 * registra en el informe y el borrado continúa. Lo que sí detiene el proceso es que falle el
 * borrado de Auth, que es el borrado de verdad.
 */
@Singleton
class DeleteAccountUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val userRegistry: UserRegistryRepository,
    private val sessionData: SessionDataCleaner
) {

    suspend operator fun invoke(): Result<DeleteAccountReport> {
        val uid = auth.currentUid()

        // Paso 1 — espejo en la nube, con tiempo límite. Va primero porque sus reglas exigen
        // ser el dueño del documento, permiso que desaparece al borrar la cuenta de Auth.
        var registryDeleted = true
        var registryFailure: String? = null
        if (uid != null) {
            val outcome = runCatching { withTimeout(NETWORK_TIMEOUT_MS) { userRegistry.delete(uid) } }
            outcome.exceptionOrNull()?.let { e ->
                registryDeleted = false
                registryFailure = when (e) {
                    is TimeoutCancellationException -> "sin respuesta del servidor"
                    else -> "${e.javaClass.simpleName}: ${e.message ?: "sin detalle"}"
                }
            }
        }

        // Paso 2 — estado local. Nunca falla de forma bloqueante.
        sessionData.clearAll()

        // Paso 3 — punto de no retorno. Con tiempo límite: si la red no responde, el usuario
        // debe ver un error accionable, no una pantalla que no reacciona.
        val deleted = runCatching { withTimeout(NETWORK_TIMEOUT_MS) { auth.deleteAccount() } }
            .getOrElse { e ->
                return Result.failure(
                    if (e is TimeoutCancellationException) {
                        IllegalStateException("Auth delete timed out")
                    } else {
                        e
                    }
                )
            }
        deleted.exceptionOrNull()?.let { return Result.failure(it) }

        // Paso 4 — la app queda usable como invitado. Con tiempo limite y sin bloquear: si la
        // red no responde, el usuario ya consta como "sin cuenta" —el repositorio lo dejo asi
        // nada mas borrar— y la sesion anonima se creara sola en el proximo arranque.
        runCatching { withTimeout(NETWORK_TIMEOUT_MS) { auth.ensureAnonymous() } }
        return Result.success(DeleteAccountReport(registryDeleted, registryFailure))
    }

    private companion object {
        /** Ninguna operación de red debe poder colgar el borrado indefinidamente. */
        const val NETWORK_TIMEOUT_MS = 10_000L
    }
}
