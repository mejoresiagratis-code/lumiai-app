package com.mejoresiagratis.lumiai.data.registry

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistrySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro central de usuarios en Cloud Firestore (colección `users`, un documento por uid).
 *
 * IMPORTANTE — esto es un ESPEJO, no la autoridad de acceso: la app nunca lee este documento
 * para decidir si desbloquear una función Pro. El gating real vive en
 * [com.mejoresiagratis.lumiai.data.billing.PlayBillingRepository], que consulta directamente a
 * Google Play. Este registro solo sirve para que tú (el negocio) veas de un vistazo en la
 * consola de Firebase quién se ha registrado, sus datos de facturación y si está suscrito —
 * util para soporte, contactar usuarios o analizar conversión. Las reglas de seguridad
 * (`firestore.rules`) restringen cada documento a su propio dueño; nadie puede leer el
 * registro de otro usuario ni auto-concederse `isSubscribed=true` para desbloquear nada.
 */
@Singleton
class FirestoreUserRegistryRepository @Inject constructor() : UserRegistryRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore

    override suspend fun sync(snapshot: UserRegistrySnapshot) {
        val ref = firestore.collection(USERS_COLLECTION).document(snapshot.uid)

        // createdAt solo se fija la primera vez (lectura previa barata; no hay Cloud Functions
        // en este proyecto todavia, asi que se resuelve del lado del cliente con un merge seguro).
        val exists = runCatching { ref.get().await().exists() }.getOrDefault(false)

        val data = buildMap<String, Any?> {
            put("uid", snapshot.uid)
            put("email", snapshot.email)
            put("fullName", snapshot.fullName)
            put("billingCountry", snapshot.billingCountry)
            put("isSubscribed", snapshot.isSubscribed)
            put("subscriptionProductId", snapshot.subscriptionProductId)
            put("updatedAt", FieldValue.serverTimestamp())
            if (!exists) put("createdAt", FieldValue.serverTimestamp())
        }

        ref.set(data, SetOptions.merge()).await()
    }

    /**
     * Borra el documento del usuario. NO se envuelve en runCatching a proposito: quien llama
     * (el borrado de cuenta) tiene que distinguir entre "borrado" y "no se pudo" para poder
     * decirselo al usuario. Las reglas de Firestore exigen ser el dueño del documento, asi que
     * esto debe ejecutarse ANTES de eliminar la cuenta de Auth.
     */
    override suspend fun delete(uid: String) {
        firestore.collection(USERS_COLLECTION).document(uid).delete().await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
