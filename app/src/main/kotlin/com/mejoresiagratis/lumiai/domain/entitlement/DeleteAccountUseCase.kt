package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Borrado de cuenta COMPLETO y en orden (17-ago).
 *
 * ## El defecto que corrige
 * Antes solo se eliminaba el usuario de Firebase Auth. El documento del usuario en Firestore
 * sobrevivía indefinidamente, igual que el perfil local y el progreso de anuncios. La política
 * de privacidad publicada afirma que se suprimen los datos asociados: era **falso**.
 *
 * ## Por qué este orden exacto
 * 1. **Firestore primero.** Las reglas de seguridad exigen ser el dueño del documento, y ese
 *    permiso desaparece con la cuenta de Auth. Borrar Auth antes dejaría el documento huérfano
 *    y ya sin credenciales para eliminarlo: basura permanente e imborrable.
 * 2. **Datos locales después**, mientras aún se sabe de quién eran.
 * 3. **Auth al final**, que es el punto de no retorno.
 * 4. **Sesión anónima**, para que la app siga usable tras el borrado.
 *
 * ## Fallo parcial
 * Si Firestore falla, se aborta SIN tocar Auth y se devuelve el error. Es deliberado:
 * preferible que el usuario reintente a decirle "cuenta borrada" dejando sus datos en la nube.
 * El punto de no retorno no se cruza hasta que lo anterior está hecho.
 */
@Singleton
class DeleteAccountUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val userRegistry: UserRegistryRepository,
    private val sessionData: SessionDataCleaner
) {

    suspend operator fun invoke(): Result<Unit> {
        val uid = auth.currentUid()

        // Paso 1 — Registro en la nube. Su fallo SÍ aborta: sin esto no podemos prometer el borrado.
        if (uid != null) {
            val remote = runCatching { userRegistry.delete(uid) }
            if (remote.isFailure) {
                return Result.failure(
                    remote.exceptionOrNull() ?: IllegalStateException("No se pudo borrar el registro")
                )
            }
        }

        // Paso 2 — Estado local. No aborta: si falla, el borrado de la cuenta sigue siendo lo correcto.
        sessionData.clearAll()

        // Paso 3 — Punto de no retorno. Puede exigir re-autenticación reciente; ese camino ya existe
        //    en la interfaz y su error se propaga tal cual para que la UI lo gestione.
        val deleted = auth.deleteAccount()
        if (deleted.isFailure) return deleted

        // Paso 4 — La app queda usable como invitado.
        runCatching { auth.ensureAnonymous() }
        return Result.success(Unit)
    }
}
