# Hito D · Cuenta lista para Google Play (RGPD)

Fecha: 23 jun 2026 · HEAD base: `13cc437`
Objetivo: cerrar la **Fase 3 (robustez)** del roadmap de producto: no perder el uid al
crear cuenta, verificación de correo, restablecer contraseña, reautenticación y
**borrado de cuenta (RGPD)** — requisitos reales de tienda.

## Estado actual (real, leído del repo)
- `AuthRepository`: `ensureAnonymous`, `signInWithEmail`, `registerWithEmail`,
  `signInWithGoogleIdToken`, `signOut`, `currentUser: Flow<AuthUser?>`, `googleWebClientId`.
- `AuthUser(uid, email, isAnonymous)` — **sin** `isEmailVerified`.
- `FirebaseAuthRepository`: `register`/`signIn`/Google usan
  `create/sign-in-WithCredential`, que **sustituyen** la sesión anónima → **se pierde el uid**.
- Entitlements: `hasAccount = user != null && !user.isAnonymous` (derivado del flow).
- UI: `AuthViewModel` (loading/failed/done genérico), `AuthScreen` (email/registro/Google),
  `AccountViewModel` (`user`, `signOut`→anónima), sección Cuenta en `SettingsScreen`
  (invitado → "Iniciar sesión"; con cuenta → email/uid + "Cerrar sesión").

## Gaps a cubrir
1. **Vincular** anónima → permanente (email y Google) conservando uid.
2. **Verificación de correo** (enviar + reflejar estado).
3. **Restablecer contraseña** por correo.
4. **Reautenticación** (login reciente) para operaciones sensibles.
5. **Borrar cuenta** (RGPD): borrar el usuario Firebase + limpiar datos locales + volver a anónima.
6. **Errores tipados** traducidos (hoy todo es "failed").
7. *(Fuera de alcance de D)* Firebase **App Check** — necesita config de consola; va aparte.

---

## Diseño del repositorio (nuevos métodos en `AuthRepository`)
```
val currentUser: Flow<AuthUser?>            // AuthUser += isEmailVerified
suspend fun registerWithEmail(...)          // ahora LINK si anónima, si no create
suspend fun signInWithGoogleIdToken(...)    // ahora LINK si anónima, si no signIn
suspend fun sendEmailVerification(): Result<Unit>
suspend fun reloadUser()                    // refrescar isEmailVerified
suspend fun sendPasswordReset(email): Result<Unit>
suspend fun reauthenticateWithPassword(password): Result<Unit>
suspend fun deleteAccount(): Result<Unit>   // delete() + ensureAnonymous()
```
Errores: `sealed class AuthError { InvalidCredentials, EmailInUse, WeakPassword,
RecentLoginRequired, Network, Unknown }` mapeando excepciones Firebase
(`FirebaseAuthUserCollisionException`, `…InvalidCredentialsException`,
`…WeakPasswordException`, `…RecentLoginRequiredException`, `FirebaseNetworkException`).
Cambiar `Result<Unit>` → `Result<Unit>` con la excepción mapeada, o devolver
`AuthError?` en el ViewModel. Plan: el repo lanza/propaga, el ViewModel mapea a `AuthError`.

---

## Tareas (un commit por tarea)

### D1 · `AuthUser.isEmailVerified` + reload
- `AuthUser` += `isEmailVerified: Boolean`.
- `currentUser` flow lee `fa.currentUser?.isEmailVerified`.
- `reloadUser()` (`currentUser.reload().await()`) para refrescar tras verificar.
- Sin UI todavía; habilita D3.

### D2 · Linking anónima → permanente (núcleo "no perder uid")
- `registerWithEmail`: si `auth.currentUser?.isAnonymous == true` →
  `currentUser.linkWithCredential(EmailAuthProvider.getCredential(email, pass))`;
  si no, `createUserWithEmailAndPassword`.
  - Colisión (`FirebaseAuthUserCollisionException`): el email ya existe → devolver
    `AuthError.EmailInUse` (no romper; el usuario puede "Iniciar sesión" en su lugar).
- `signInWithGoogleIdToken`: si anónima → `linkWithCredential(Google)`;
  - Colisión: esa cuenta Google ya existe → **fallback** a `signInWithCredential`
    (se entra a la cuenta existente; los datos anónimos se descartan, es lo esperado).
  - si no anónima → `signInWithCredential` como hoy.
- `signInWithEmail` se queda igual (entrar a cuenta existente; sustituir anónima es correcto).

### D3 · Verificación de correo + restablecer contraseña
- Repo: `sendEmailVerification()`, `sendPasswordReset(email)`.
- Tras `registerWithEmail` con éxito → enviar verificación automáticamente.
- UI `AuthScreen`: enlace **"¿Olvidaste tu contraseña?"** (pide email → `sendPasswordReset`),
  y aviso "te hemos enviado un correo de verificación" tras registrar.
- UI Ajustes (con cuenta de email no verificada): fila de estado + botón
  **"Reenviar verificación"**; `reloadUser()` al volver a la pantalla.

### D4 · Reautenticación + borrar cuenta (RGPD)
- Repo: `reauthenticateWithPassword(pass)`, `deleteAccount()`
  (`currentUser.delete()`; si `RecentLoginRequired` → propagar `AuthError.RecentLoginRequired`;
  al borrar OK → `ensureAnonymous()` para que la app siga usable).
- Limpiar datos locales del usuario en el borrado (DataStore de preferencias/ajustes que sean
  personales; los ajustes de modo pueden conservarse o resetearse — decisión: resetear lo
  asociado a cuenta, mantener preferencias de UI neutras).
- UI Ajustes (con cuenta): botón **"Borrar cuenta"** (estilo peligro) →
  **diálogo de confirmación** → `deleteAccount()`.
  - Si `RecentLoginRequired`: pedir reautenticación.
    - Email: diálogo de contraseña → `reauthenticateWithPassword` → reintentar borrado.
    - Google: re-lanzar el flujo de Google (idToken fresco) → `reauthenticate` → borrar.
      *(Reusa el flujo Google existente de AuthScreen; es la parte más delicada.)*

### D5 · Errores tipados → mensajes en español
- `AuthUiState` += `error: AuthError?` (sustituye el `failed` genérico, o lo complementa).
- Mapear en el ViewModel y mostrar strings: credenciales inválidas, email en uso,
  contraseña débil, se requiere inicio reciente, sin conexión, error desconocido.
- Strings ES nuevos para cada caso y para verificación/reset/borrado.

---

## Bordes y seguridad
- **Anónimo sin email**: no puede verificar ni resetear; la UI solo ofrece esas acciones a
  cuentas de email reales.
- **`delete()` requiere login reciente** → siempre preparar el camino de reauth.
- **Borrado es irreversible** → confirmación explícita obligatoria.
- **Colisión en linking** → mensaje claro, nunca crash.
- **RGPD**: al borrar el `FirebaseUser` se elimina el dato personal de Auth; no hay backend
  propio con PII adicional (entitlements se derivan, no se almacenan en servidor). Documentarlo
  en la política de privacidad (Fase 6).
- Verificación estática (sin build local): firmas exactas de Firebase BoM 33.5.1, balance de
  llaves, imports; pruebas en dispositivo las hace Pablo (registrar→verificar, reset, borrar).

## Orden sugerido
D1 → D2 → D3 → D4 → D5. D2 es el corazón ("no perder uid"); D4 el más sensible (borrado).
Cada uno deja la app compilando; D5 pule los mensajes al final.

## Fuera de alcance (siguiente)
Firebase App Check (config de consola), ficha/política de privacidad (Fase 6).
