# Fase 3 — Cuenta (Firebase Auth) + framework de entitlements · Implementation Plan

> Se implementa tarea a tarea; cada una deja el proyecto compilando. No toca el motor de linterna.

**Goal:** Identidad de usuario con Firebase Auth (anónimo por defecto + correo/contraseña + Google) y una **fuente única de verdad de permisos** (`EntitlementRepository`) que decide qué modos están disponibles según cuenta/suscripción/desbloqueos temporales. Sección "Cuenta" en Ajustes y pantalla de acceso. Base para anuncios recompensados (Fase 4) y suscripción (Fase 5).

**Modelo de entitlements (confirmado):**
- Básicos (Continuo/Pantalla/SOS/Estrobo): siempre.
- Avanzados normales: con **cuenta** (sesión no anónima) o suscripción; o desbloqueo temporal (2 ads = 1 h del modo seleccionado, Fase 4).
- Avanzados con IA: solo con **suscripción** (Fase 5).
- Hoy no hay modos avanzados implementados: la Fase 3 deja el framework + la UI de cuenta; el bloqueo se "verá" al añadir modos avanzados.

**Architecture:** capa pura testeable `Entitlements.unlocks(tier)`; `AuthRepository` (interfaz) con impl `FirebaseAuthRepository` (firebase-auth-ktx, `authStateListener` → Flow). `EntitlementRepository` deriva de `AuthRepository`. Google Sign-In con **Credential Manager** usando el `web client id` del JSON; si no existe (JSON sin OAuth web), el botón Google se oculta. `ensureAnonymous()` al arrancar para que todos tengan uid. Reglas `ui-ux-pro-max`: tokens, targets ≥48dp, errores claros.

**Tech Stack:** Firebase Auth (BoM) · Google Services plugin · Credentials/GoogleId · Compose · Hilt · DataStore.

---

## Prerrequisitos en consola (para que Google funcione)

1. Authentication → Sign-in method → habilitar **Anónimo**, **Correo/contraseña** y **Google**.
2. Project Settings → tu app Android → **Add fingerprint**: añade la **SHA-1** (y SHA-256) del keystore de depuración. (En tu máquina: `./gradlew signingReport`, o `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`.)
3. **Re-descarga** `google-services.json` (ya traerá el OAuth web client) y reemplaza `app/google-services.json`.

Anónimo y correo/contraseña no necesitan SHA-1; Google sí.

---

## File Structure

```
gradle/libs.versions.toml            # (modificar) versiones + libs + plugin google-services
build.gradle.kts (root)              # (modificar) plugin google-services apply false
app/build.gradle.kts                 # (modificar) aplicar plugin + deps firebase/credentials
domain/model/AuthUser.kt             # (nuevo)
domain/repository/AuthRepository.kt  # (nuevo)
data/auth/FirebaseAuthRepository.kt  # (nuevo)
domain/entitlement/Entitlements.kt   # (nuevo) modelo puro + Tier + FlashMode.tier
domain/repository/EntitlementRepository.kt        # (nuevo)
data/entitlement/DefaultEntitlementRepository.kt  # (nuevo)
di/AppModule.kt                      # (modificar) binds Auth + Entitlement
ui/auth/AuthViewModel.kt             # (nuevo)
ui/auth/AuthScreen.kt                # (nuevo) correo/contraseña + Google
ui/settings/SettingsScreen.kt        # (modificar) sección "Cuenta"
ui/settings/AccountViewModel.kt      # (nuevo) estado de cuenta para Ajustes
ui/start/StartViewModel.kt           # (modificar) ensureAnonymous al arrancar
ui/navigation/LumiAiNavHost.kt       # (modificar) ruta AUTH
app/src/test/.../EntitlementsTest.kt # (nuevo) TDD del modelo puro
res/values/strings.xml               # (modificar) textos de cuenta/acceso
```

---

## Task 1: Gradle — Firebase + Google Services + Credentials

- [ ] **libs.versions.toml** — añadir en `[versions]`:
```toml
googleServices = "4.4.2"
firebaseBom = "33.5.1"
credentials = "1.3.0"
googleid = "1.1.1"
```
en `[libraries]`:
```toml
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
```
en `[plugins]`:
```toml
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

- [ ] **build.gradle.kts (root)** — añadir en `plugins {}`:
```kotlin
    alias(libs.plugins.google.services) apply false
```

- [ ] **app/build.gradle.kts** — añadir en `plugins {}`:
```kotlin
    alias(libs.plugins.google.services)
```
y en `dependencies {}`:
```kotlin
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
```

- [ ] **Build + commit**: `./gradlew :app:assembleDebug` → OK (el plugin lee `app/google-services.json`, ya commiteado).
```bash
git commit -m "build: add Firebase Auth, Google Services plugin, Credential Manager"
```

---

## Task 2: Modelo de entitlements (puro, TDD)

- [ ] **Test que falla** (`app/src/test/.../domain/entitlement/EntitlementsTest.kt`):
```kotlin
package com.mejoresiagratis.lumiai.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    @Test fun `basic siempre desbloqueado`() {
        assertTrue(Entitlements().unlocks(Tier.BASIC))
    }
    @Test fun `advanced requiere cuenta o suscripcion`() {
        assertFalse(Entitlements(hasAccount = false, hasSubscription = false).unlocks(Tier.ADVANCED))
        assertTrue(Entitlements(hasAccount = true).unlocks(Tier.ADVANCED))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.ADVANCED))
    }
    @Test fun `ai solo con suscripcion`() {
        assertFalse(Entitlements(hasAccount = true).unlocks(Tier.AI))
        assertTrue(Entitlements(hasSubscription = true).unlocks(Tier.AI))
    }
}
```

- [ ] **Implementación** (`domain/entitlement/Entitlements.kt`):
```kotlin
package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.FlashMode

enum class Tier { BASIC, ADVANCED, AI }

/** Hoy todos los modos son básicos; los avanzados llegarán en fases siguientes. */
val FlashMode.tier: Tier
    get() = when (this) {
        FlashMode.CONTINUOUS, FlashMode.SCREEN, FlashMode.SOS_MORSE, FlashMode.STROBE -> Tier.BASIC
    }

data class Entitlements(
    val hasAccount: Boolean = false,
    val hasSubscription: Boolean = false
) {
    fun unlocks(tier: Tier): Boolean = when (tier) {
        Tier.BASIC -> true
        Tier.ADVANCED -> hasAccount || hasSubscription
        Tier.AI -> hasSubscription
    }
}
```

- [ ] Test verde + commit `feat(entitlement): pure entitlement model (tier-based)`.

---

## Task 3: AuthRepository + Firebase impl + EntitlementRepository

- [ ] **AuthUser + AuthRepository**:
```kotlin
// domain/model/AuthUser.kt
package com.mejoresiagratis.lumiai.domain.model
data class AuthUser(val uid: String, val email: String?, val isAnonymous: Boolean)
```
```kotlin
// domain/repository/AuthRepository.kt
package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    val googleWebClientId: String?
    suspend fun ensureAnonymous()
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun signOut()
}
```

- [ ] **FirebaseAuthRepository** (`data/auth/FirebaseAuthRepository.kt`):
```kotlin
package com.mejoresiagratis.lumiai.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.let { AuthUser(it.uid, it.email, it.isAnonymous) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val googleWebClientId: String?
        get() {
            val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            return if (id != 0) context.getString(id) else null
        }

    override suspend fun ensureAnonymous() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await(); Unit
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await(); Unit
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await(); Unit
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
```
> Requiere `kotlinx-coroutines-play-services` para `.await()`. Añadir lib `kotlinx-coroutines-play-services` (mismo `coroutines` version) en Task 1 deps: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:<coroutines>")` vía catálogo.

- [ ] **EntitlementRepository**:
```kotlin
// domain/repository/EntitlementRepository.kt
package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {
    val entitlements: Flow<Entitlements>
}
```
```kotlin
// data/entitlement/DefaultEntitlementRepository.kt
package com.mejoresiagratis.lumiai.data.entitlement

import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEntitlementRepository @Inject constructor(
    auth: AuthRepository
) : EntitlementRepository {
    // Fase 4/5 añadirán desbloqueos temporales y suscripción combinando más flows.
    override val entitlements: Flow<Entitlements> = auth.currentUser.map { user ->
        Entitlements(
            hasAccount = user != null && !user.isAnonymous,
            hasSubscription = false
        )
    }
}
```

- [ ] **AppModule** — añadir binds:
```kotlin
    @Binds @Singleton
    abstract fun bindAuthRepo(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindEntitlementRepo(impl: DefaultEntitlementRepository): EntitlementRepository
```

- [ ] **StartViewModel** — `ensureAnonymous()` al crear (inyectar `AuthRepository`, `init { viewModelScope.launch { auth.ensureAnonymous() } }`).

- [ ] Build + commit `feat(auth): AuthRepository (Firebase) + EntitlementRepository + ensure anonymous`.

---

## Task 4: UI — pantalla de acceso + sección Cuenta en Ajustes

- [ ] **AuthViewModel** (`ui/auth/AuthViewModel.kt`): expone `webClientId`, y funciones `signInEmail/registerEmail/signInGoogle(idToken)` que actualizan un `StateFlow<AuthUiState>` (loading/error/success); al éxito navega atrás.

- [ ] **AuthScreen** (`ui/auth/AuthScreen.kt`): campos correo/contraseña (con `OutlinedTextField`, `PasswordVisualTransformation`), botones "Entrar" y "Crear cuenta"; botón "Continuar con Google" **solo si `webClientId != null`**, que lanza Credential Manager:
```kotlin
val webId = viewModel.webClientId
// ...
val option = GetGoogleIdOption.Builder()
    .setServerClientId(webId)
    .setFilterByAuthorizedAccounts(false)
    .build()
val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
scope.launch {
    runCatching {
        val result = CredentialManager.create(context).getCredential(context, request)
        val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        viewModel.signInGoogle(token)
    }.onFailure { /* mostrar error */ }
}
```
Muestra errores de forma clara (correo inválido, contraseña corta, credenciales incorrectas).

- [ ] **AccountViewModel** + **sección "Cuenta" en SettingsScreen**:
  - Invitado/anónimo → texto "Invitado" + nota "Crea una cuenta para desbloquear los modos avanzados" + botón "Entrar / Crear cuenta" (navega a AUTH).
  - Con cuenta → muestra el correo + botón "Cerrar sesión".
  - SettingsScreen recibe `onOpenAuth: () -> Unit` (desde el NavHost).

- [ ] **NavHost** — ruta `AUTH`; `SettingsScreen(onOpenAuth = { navController.navigate(Routes.AUTH) })`; `composable(AUTH) { AuthScreen(onDone = { navController.popBackStack() }) }`.

- [ ] **Strings** (ES): `account_section`, `account_guest`, `account_guest_hint`, `account_sign_in`, `account_sign_out`, `auth_title`, `auth_email`, `auth_password`, `auth_sign_in`, `auth_register`, `auth_google`, `auth_error_generic`, etc.

- [ ] Build + commit `feat(ui): auth screen (email/Google) + account section in settings`.

---

## Task 5: Verificación

- [ ] `./gradlew :app:testDebugUnitTest` → PASS (incluye EntitlementsTest)
- [ ] `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (con google-services.json del repo)
- [ ] Push + CI verde por `head_sha`.
- [ ] Checklist on-device:
  - [ ] Primer arranque crea sesión **anónima** (Ajustes → Cuenta muestra "Invitado").
  - [ ] Crear cuenta con correo/contraseña → Ajustes muestra el correo; "Cerrar sesión" vuelve a invitado.
  - [ ] (Tras regenerar el JSON con SHA-1 + Google habilitado) "Continuar con Google" inicia sesión.
  - [ ] Reabrir mantiene la sesión.

---

## Self-Review (hecho)

- **CI verde:** `google-services.json` está commiteado, el plugin se aplica sin trucos. ✔
- **Google degradado con elegancia:** si el JSON no trae web client, el botón Google no aparece (resto funciona). ✔
- **Honestidad:** sin cámara; "cuenta" desbloquea avanzados de verdad (cuando existan); IA solo con suscripción. ✔
- **Testeable:** la lógica de permisos es pura (`unlocks(tier)`), con tests; Firebase queda aislado tras interfaz. ✔
- **Riesgo:** Credential Manager/GoogleId requiere Play Services en el dispositivo; en emuladores sin Play, Google no estará disponible (anónimo/correo sí). Linkeo de cuenta anónima→cuenta real se pospone (ahora sign-in directo).
