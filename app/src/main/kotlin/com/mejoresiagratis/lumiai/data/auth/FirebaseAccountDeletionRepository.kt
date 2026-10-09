package com.mejoresiagratis.lumiai.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.repository.AccountDeletionRepository
import com.mejoresiagratis.lumiai.domain.repository.PendingAccountDeletion
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAccountDeletionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AccountDeletionRepository {
    private val uidKey = stringPreferencesKey("account_deletion_uid")
    private val receiptKey = stringPreferencesKey("account_deletion_receipt")
    private val confirmedKey = booleanPreferencesKey("account_deletion_confirmed")
    override val pending = dataStore.data.map { prefs ->
        prefs[uidKey]?.let { PendingAccountDeletion(it, prefs[receiptKey].orEmpty(), prefs[confirmedKey] == true) }
    }
    override suspend fun savePending(value: PendingAccountDeletion) {
        dataStore.edit { it[uidKey] = value.uid; it[receiptKey] = value.receipt; it[confirmedKey] = value.serverConfirmed }
    }
    override suspend fun clearPending() {
        dataStore.edit { it.remove(uidKey); it.remove(receiptKey); it.remove(confirmedKey) }
    }
    override suspend fun deleteRemotely(pending: PendingAccountDeletion) {
        val uid = pending.uid
        val user = FirebaseAuth.getInstance().currentUser
        check(user?.uid == uid) { "Account changed" }
        // Reauthentication updates auth_time; force the new token before calling the server.
        user!!.getIdToken(true).await()
        try {
            val result = FirebaseFunctions.getInstance("europe-west1")
                .getHttpsCallable("deleteMyAccount")
                .apply { setTimeout(30, TimeUnit.SECONDS) }
                .call(mapOf("expectedUid" to uid, "receipt" to pending.receipt)).await()
            check((result.data as? Map<*, *>)?.get("status") == "completed") {
                "Deletion was not confirmed"
            }
        } catch (error: FirebaseFunctionsException) {
            if ((error.details as? Map<*, *>)?.get("reason") == "recent-login-required") {
                throw AuthException(AuthError.RecentLoginRequired)
            }
            throw error
        }
    }
    override suspend fun isCompleted(pending: PendingAccountDeletion): Boolean {
        val token = FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
        val project = FirebaseApp.getInstance().options.projectId ?: return false
        return withContext(Dispatchers.IO) {
            val connection = URL("https://europe-west1-$project.cloudfunctions.net/accountDeletionStatus")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Firebase-AppCheck", token)
                val body = JSONObject().put("uid", pending.uid).put("receipt", pending.receipt).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode != 200) false
                else connection.inputStream.bufferedReader().use {
                    JSONObject(it.readText()).optString("status") == "completed"
                }
            } finally { connection.disconnect() }
        }
    }

}
