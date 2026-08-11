# Memorándum de fallos de build/CI — LumiAI

> **Regla de uso:** ANTES de cada `git push`, leer este fichero y pasar el _checklist_.
> DESPUÉS de cada fallo de build/CI, **añadir** una entrada en "Registro" y, si aplica,
> una regla nueva al _checklist_. No basta con arreglar el error del momento: hay que
> capturar la lección para no repetirla.
>
> Meta-lección (origen de este memo): **no enfocarse solo en lo recién implementado.**
> Un cambio puede romper código/tests que ya funcionaban. Antes de escribir código nuevo,
> **leer todos los ficheros implicados** (incluido el que ya funciona) y reutilizar/declarar
> desde ahí.

## Checklist pre-push (derivado de fallos reales)

1. **Balances y claves**: llaves/paréntesis equilibrados; imports presentes; cada
   `R.string.X` referenciado existe en `values/strings.xml` (y `values-es/` con paridad).
2. **Receptor de `getString` / `Context`**: `getString(...)` solo resuelve donde hay un
   `Context` implícito (Activity, Service, Composable vía `stringResource`). En
   **`companion object`, funciones top-level o lambdas sin receptor Context** hay que usar
   `context.getString(...)` con un parámetro/variable `Context` explícito.  → Fallos #1/#2.
3. **Cambios en `strings.xml` o en el locale por defecto**: si cambias VALORES o el idioma
   por defecto (p. ej. `values/` pasa a inglés), **buscar tests que asserten literales**:
   `onNodeWithText("…")`, `onNodeWithContentDescription("…")`,
   `expectValue(StateDescription, "…")`, `assertTextEquals("…")`.
   Los tests de UI deben leer el recurso (`RuntimeEnvironment.getApplication().getString(R.string.X)`),
   NO fijar el texto. → Fallo #3.
4. **Firmas / constructores cambiados**: `grep -rn "NombreClase(" app/src/test` para ver si
   algún test construye con la firma vieja. Si un modo nuevo declara `requiresX()=true`,
   buscar tests tipo "hoy ningun modo exige X" (`grep -rn "requires" app/src/test`).
5. **Patrones de vibración/flash (`LongArray`)**: longitud **par** (pares on/off).
6. **Codec / parsing**: una entrada inválida se descarta **entera** (no a medias).
7. **Imports duplicados**: al insertar imports por script, el fichero puede tenerlos
   ya en OTRA posición (no adyacente) → `Conflicting import ... is ambiguous`.
   `pre_push_audit.sh` ahora lo detecta (`sort | uniq -d` sobre las líneas import).
   → Fallo #4.
8. **Recordatorio**: el sandbox NO compila; estas comprobaciones estáticas no detectan
   errores de lógica/tipos finos. Leer los ficheros reales reduce el riesgo, pero el QA y el
   CI los confirma Pablo.

## Registro de fallos

### 2026-06-28
- **#1** run `76473132888` · commit `cbc7a2f` · `compileDebugKotlin` FAILED ·
  `SoundAlertService.kt:190 Unresolved reference 'getString'`.
  **Causa:** `getString(R.string.sa_title)` dentro de `ensureChannel(context: Context)`, que
  vive en el **companion object** (sin receptor `Context`).
  **Fix:** `db1e2c9` → `context.getString(...)`.
- **#2** run `76473460455` · mismo commit/causa que #1 (re-run).
- **#3** run `76473697506` (posterior al fix de compile) · `testDebugUnitTest` FAILED ·
  `PowerOrbSemanticsTest` 3 tests (AssertionError 46/56/66).
  **Causa:** al pasar `values/` a **inglés por defecto** (i18n EN/ES), Robolectric resuelve
  los strings en inglés; el test fijaba literales en español (`"Linterna"`, `"Apagada"`,
  `"Encendida"`) → dejaron de casar.
  **Fix:** el test lee `R.string.a11y_torch/a11y_state_on/a11y_state_off` vía
  `RuntimeEnvironment.getApplication()`, locale-independiente.
  **Lección:** cambiar el locale por defecto rompe cualquier test que assertee TEXTO;
  los asserts de UI deben ir contra recursos, no contra literales (→ checklist #3).

### 2026-07-24
- **#4** run `81619888724` · commit `4ad0ce9` · `compileDebugKotlin` FAILED ·
  `BeamHubScreen.kt:106/108 Conflicting import: imported name 'Tier' is ambiguous`.
  **Causa:** un script de edición insertó `import ...entitlement.Tier` sin ver que el
  fichero ya lo importaba unas líneas más abajo (duplicados NO adyacentes; el dedup
  del script solo cubría líneas consecutivas).
  **Fix:** dedup real de imports + chequeo permanente en `pre_push_audit.sh`.
  **Lección:** los chequeos de "ya existe este import" deben mirar el fichero entero,
  no el vecindario de la inserción (→ checklist #7).

- **#5** run `81620701292` · commit `4ad0ce9` (musica al carrusel) ·
  `testDebugUnitTest` FAILED · `ModeAvailabilityTest` 2 tests
  (`ningun modo actual requiere microfono` / `la presencia de microfono no
  cambia...`).
  **Causa:** tests-contrato del F0 de Alerta Sonora que fijaban "hoy ningun
  modo exige microfono"; Musica (v0.3.1) ya declara `requiresMicrophone()=true`
  y los dejo obsoletos. **El APK se genero igual** (assembleDebug corrio
  tras el fallo de test y crashea el job solo por el resultado de tests).
  **Fix:** tests actualizados al contrato actual (Musica exige microfono;
  el resto no) en vez de "ningun modo".
  **Lección:** al anadir `requiresX()=true` a un modo nuevo, buscar tests
  que fijen el contrato "hoy ningun modo exige X" y actualizarlos en el
  mismo commit que el modo (→ checklist #4, ampliado).

### 2026-07-25
- **#6** run `81688341037` · commit `9ab190e` · `hiltJavaCompileDebug` FAILED ·
  `IllegalArgumentException: Provided Metadata instance has version 2.2.0, while
  maximum supported version is 2.1.0` (stack en `dagger.spi.internal.shaded...
  kotlinx.metadata.jvm`).
  **Causa:** Hilt 2.52 lleva shadeado `kotlinx-metadata-jvm` que solo lee metadata
  Kotlin hasta 2.1.0. `billing-ktx:9.1.0` (añadida en v0.6.0) está compilada con
  Kotlin 2.2.x → el procesador de Dagger truena al escanear el classpath, sin que
  haya código nuestro implicado. El fallo no aparece al añadir la dependencia en
  el catálogo sino en el primer build que la procesa.
  **Fix (v0.6.2):** bump coordinado mínimo: `hilt 2.52 → 2.57.1` (kotlin-metadata-jvm
  des-shadeado, soporta metadata nuevas), `kotlin 2.0.21 → 2.1.10` y
  `ksp 2.0.21-1.0.28 → 2.1.10-1.0.31` (mínimo exigido por Hilt 2.56+). AGP 8.7.3,
  Gradle 8.9 y Compose BOM 2024.09.03 se quedan como están (compatibles).
  La rama parkeada `chore/toolchain-modernization-a1` NO es mergeable: base
  pre-v0.6.0 (sin billing/firestore); solo sirvió de referencia de matriz.
  **Lección:** cualquier dependencia nueva puede arrastrar metadata de un Kotlin
  más nuevo que el que soporta nuestro annotation processor; al añadir libs de
  Google recientes, verificar la versión de Kotlin con la que están compiladas
  contra el máximo que soporta Hilt/kapt (→ checklist #8, nuevo).

- **#7** run `81689652573` · commit v0.6.2 · `testDebugUnitTest` FAILED ·
  6 tests de `ModePillSemanticsTest`/`PowerOrbSemanticsTest` con
  `IllegalStateException at FirebaseApp.java:179` ("Default FirebaseApp is not
  initialized").
  **Causa:** fallo LATENTE de v0.6.0, destapado al arreglar #6. Robolectric
  instancia la Application real del manifest (`LumiAiApplication`,
  `@HiltAndroidApp`), que desde v0.6.0 inyecta `AuthRepository` y
  `UserRegistryRepository`; sus constructores llaman `Firebase.auth` /
  `Firebase.firestore` en la creación del grafo → `FirebaseApp.getInstance()`
  truena porque `FirebaseInitProvider` no corre en Robolectric. Los builds
  v0.6.0/v0.6.1 nunca llegaron a la fase de tests (rompían antes en #6), por
  eso no se vio.
  **Fix:** `@Config(sdk = [34], application = Application::class)` en ambos
  tests de semántica: usan una Application plana de android.app, sin Hilt ni
  Firebase, que es todo lo que necesitan para renderizar composables sueltos.
  **Lección:** todo test Robolectric que renderice composables debe fijar
  `application = Application::class` (o un TestApplication propio) en su
  `@Config`; nunca depender de la Application del manifest, que arrastra el
  grafo Hilt completo y sus efectos de onCreate (→ checklist #9, nuevo).
  Además: cuando un fallo de build tape la fase de tests durante varias
  versiones, esperar fallos latentes al destaparlo.

### 2026-07-25 (mejora de CI, no es fallo)
- **CI aligerado (Opción B):** `testDebugUnitTest assembleDebug` seguía corriendo los
  2 tests Robolectric de semántica Compose en cada push (~2-2.5 min de los ~3 del job:
  descargan el JAR de Android y arrancan entorno emulado en JVM). Se marcan con
  `@Category(com.mejoresiagratis.lumiai.testing.SlowTest)` y se excluyen del camino
  crítico con `-PexcludeSlowTests` (config en `app/build.gradle.kts`, `tasks.withType<Test>`).
  Los tests de dominio (rápidos) siguen corriendo y protegiendo tiers/flash. En local,
  sin la flag, corre TODO. Quitado también `--stacktrace` del paso feliz (solo ruido).
  Comentario obsoleto de Kotlin 2.0.21 en build.gradle actualizado a 2.1.10.
  **Nota:** los Robolectric siguen existiendo y son válidos; solo salen del gate de push.
  Para correrlos en CI: lanzar `quality`/dispatch manual o quitar la flag puntualmente.

### 2026-07-25 (bug de UI en dispositivo, no de build)
- **Texto invertido en campos de facturación** (captura de Pablo: "Pablo"->"abloP",
  "España"->"spañaE"). **Causa:** los dos `OutlinedTextField` de Ajustes tenían
  `value = billingProfile.fullName/billingCountry` (un String que viene de DataStore
  vía Flow) y `onValueChange` disparaba `viewModelScope.launch { DataStore.edit }`.
  Al ser la persistencia ASÍNCRONA, Compose recomponía con el value viejo en cada
  tecla: se perdía la posición del cursor y las escrituras rápidas se reordenaban.
  **Antipatrón:** cablear el `value` de un TextField a un flujo asíncrono/persistido.
  **Fix (Opción A):** nuevo composable `PersistedTextField` con estado local
  `TextFieldValue` (preserva cursor/selección) que solo persiste en `onFocusChanged`
  al perder el foco, y re-sincroniza desde fuera solo cuando NO tiene el foco.
  **Lección:** el estado de edición de un TextField debe ser local y síncrono; el
  guardado a DataStore/red va con debounce o al perder foco, nunca por pulsación
  (→ checklist #10, nuevo).

### 2026-07-25 (bug crítico de UI en dispositivo — modo Pantalla en blanco)
- **Modo Pantalla abría en blanco sin controles** (captura de Pablo: pantalla blanca
  total, sin panel de ajustes ni forma de tocar nada). **Causa raíz:** en `ScreenLight.kt`
  el estado de bloqueo era `var locked by rememberSaveable { mutableStateOf(autoLockScreen) }`.
  `rememberSaveable` PERSISTE en el Bundle de la Activity: una vez que `locked` valía `true`
  (usuario tocó el candado, o auto-lock activo en una sesión), ese `true` se restauraba en
  CADA reapertura del modo, incluso tras cerrar. El overlay `if (locked)` cubre toda la
  pantalla (`matchParentSize`) con fondo opaco `Color(argb)` (blanco por defecto, screenArgb
  = -0x1) tapando el panel de ajustes entero; el "mantén pulsado para desbloquear" no era
  descubrible.
  **Fix (2 partes):**
  (1) `remember(autoLockScreen) { mutableStateOf(autoLockScreen) }` — el bloqueo se reinicia
      según el ajuste REAL de auto-bloqueo cada vez que se entra al modo; nunca restaura un
      `true` viejo. Quitado el import huérfano de `rememberSaveable`.
  (2) overlay de bloqueo con velo `Color.Black.copy(alpha=0.55f)` (no opaco al color de
      pantalla) + candado/textos en blanco → siempre legibles y con salida evidente.
  **Lección:** `rememberSaveable` para estado de UI efímero (bloqueo, expandido, overlays)
  es peligroso: persiste entre recreaciones y "atrapa" al usuario en un estado del que no
  puede salir. Usar `remember` (con clave si depende de un ajuste) salvo que la persistencia
  entre rotaciones sea deseada Y haya salida garantizada (→ checklist #12, nuevo).
  Nota: BeamHubScreen usa `collectAsStateWithLifecycle` (el memo Set A prefiere
  `collectAsState`); funciona aquí y no era la causa, se deja pero queda anotado.

### 2026-07-25 (robustez de Música + latido de Íntimo — QA en dispositivo)
- **Modo Íntimo: efecto "latido" poco visible.** La animación RESPIRACION iba de alpha
  0.55→1.0 con LinearEasing y ciclo de 5 s: cambio por instante mínimo, se percibía
  apenas. **Fix:** keyframes con envolvente orgánica (FastOutSlowInEasing), rango
  ampliado 0.40→1.0 y un doble pulso (sístole/diástole) en el pico, ciclo de 4 s.
  "Ambos, más marcado pero suave · claramente visible" (elección de Pablo). Quitado
  import huérfano de LinearEasing.
- **Modo Música: robustez operacional del FGS** (el DSP del BeatDetector ya era sólido;
  el problema era resiliencia del servicio). Añadido en `MusicFlashService`:
  · manejo de códigos de error negativos de `AudioRecord.read()` (DEAD_OBJECT, etc.) con
    watchdog (MAX_READ_ERRORS=20 + backoff) → no gira en vacío para siempre; para limpio.
  · `startRecording()` y constructor de AudioRecord protegidos (mic ocupado / args malos)
    → notifica error y para en vez de crashear.
  · gestión de FOCO DE AUDIO: al perderlo (llamada, otra grabadora, asistente) se pausan
    los destellos y se apaga el LED; se reanuda al recuperarlo. AudioFocusRequest guardado
    para API 26+ con fallback deprecated en 24-25 (minSdk 24).
  · notificación FGS con estados (escuchando / en pausa / error) vía updateNotification.
  · 2 strings nuevas x2 idiomas (255/255).
  **Lección:** un bucle de captura de audio/sensor debe tratar SIEMPRE los códigos de
  error del read (no solo `>0`), tener watchdog contra el giro en vacío, y ceder el flash
  al perder el foco de audio. La lógica DSP correcta no basta para producción sin la
  resiliencia del servicio que la aloja (→ checklist #13, nuevo).

### 2026-07-25
- **#8** run `81695364271` · commit `a46295b` · `compileDebugKotlin` FAILED ·
  `Unresolved reference 'pro_progress_one_left' / 'pro_progress_start' /
  'pro_watch_ad_last'` en SettingsScreen.kt (líneas 373/374/410).
  **Causa:** entrega parcial en Codespaces. El ZIP `lumiai-pro-prefill-cta-progreso`
  contenía SettingsScreen.kt (con los usos de las 3 strings nuevas) Y strings.xml/
  strings-es (con sus definiciones), pero al aplicar el ZIP solo se actualizó el .kt;
  ambos strings.xml quedaron en la versión ANTERIOR (ni las 3 defs nuevas ni el copy
  CTA reescrito). El .kt referenciaba recursos inexistentes -> no compila. El fallo no
  se vio en el push de ese commit porque el CI de ese momento no llegó / se solapó con
  los commits siguientes (pantalla, intimo/musica), que sí se pushearon completos.
  **Fix:** reaplicadas las 3 strings nuevas + el copy CTA (AIDA/PAS) en EN y ES sobre
  el estado real del remoto. Verificado con `comm -23` sobre TODO app/src (no solo
  Settings): 0 R.string usadas sin definir. i18n 258/258.
  **Lección:** al aplicar un ZIP de entrega con `unzip -o`, si un fichero no se
  sobreescribe (permiso, ruta, conflicto), se rompe la atomicidad del commit y quedan
  usos sin sus recursos. El gate `comm -23` de R.string usadas-vs-definidas debe correr
  sobre TODO el árbol y ANTES de push, no solo sobre el fichero editado (→ checklist #7
  ampliado: el chequeo de recursos es global, no por-fichero). Además: tras un `unzip -o`
  en el entorno de entrega, `git status` debe mostrar TODOS los ficheros del ZIP como
  modificados; si falta alguno, la entrega quedó parcial.

### 2026-07-25 (feature nueva — modo Letrero LED)
- **Letrero LED** (petición de Pablo, referencia: apps de letrero tipo "LED banner").
  Marquesina de texto con estética de panel de puntos, emojis incluidos, fuera del
  carrusel: sección propia en Ajustes + ruta LED_BANNER, gate idéntico a Alerta Sonora
  (Pro O desbloqueo temporal por 2 anuncios, vía proUnlocked/RewardedUnlockViewModel).
  **Arquitectura de renderizado (skill chrisbanes/compose-animations, 559 installs):**
  · el texto se RASTERIZA UNA VEZ por config (Paint a 4x -> reescalado a rejilla de 26
    filas -> IntArray de píxeles); nada se mide ni rasteriza por frame. Emojis gratis.
  · scroll = acumulador withFrameNanos leído DENTRO del bloque draw del Canvas
    (deferred read): 60fps invalidando solo la fase de dibujo, cero recomposiciones.
  · dibujo acotado: filas x columnas visibles círculos por frame.
  **Persistencia:** LedBannerConfig (texto/color/velocidad 1-10/dirección) en DataStore
  (repo + @Binds en AppModule + HiltViewModel). TextField con estado LOCAL (checklist
  #10 aplicado desde el diseño). Reglas del memo respetadas: collectAsState, sin imports
  de DrawScope, literales Color con sufijo L en fichero con Canvas.
  16 strings x2 idiomas (273/273). Display fullscreen: brillo máx + KEEP_SCREEN_ON con
  restauración en onDispose, toca para salir, BackHandler.

### 2026-07-25 (falso positivo diagnosticado + endurecimiento de God mode)
- **Síntoma reportado:** la app arrancaba en Música (no en Continuo) y TODOS los modos
  aparecían desbloqueados sin login ni pago. **NO era una regresión de ninguna versión.**
  **Causa única para ambos síntomas:** el override de superusuario (God) estaba activo en
  DataStore (`dbg_force_account` / `dbg_force_subscription`). Fuerza hasAccount=true y
  hasSubscription=true en builds debug (que es lo que produce el CI con assembleDebug):
  (a) desbloquea todos los tiers; (b) el modo SIEMPRE se ha persistido, y el guard de
  BeamHubScreen (`if (!access.unlocks(mode.tier)) selectMode(CONTINUOUS)`) es lo que antes
  devolvía a Continuo; con todo desbloqueado ese guard ya no dispara y Música se queda.
  El override existe desde v0.1.0 (`8dc97fa`) y siempre estuvo tras `BuildConfig.DEBUG`.
- **Endurecimiento aplicado (decisión: God SOLO en debug, nunca en release):**
  · la ruta `Routes.GOD` ya no se registra en el NavHost en release (antes el composable
    se registraba siempre: navegable por deep link o por un navigate() olvidado) y el
    callback onOpenGod también queda gateado.
  · `LumiAiApplication.purgeGodOverrideOnRelease()`: al arrancar un build release se borran
    las claves del override, por si quedaron de una instalación debug con el mismo
    applicationId. Antes se ignoraban pero seguían escritas.
  · aviso VISIBLE en Ajustes (solo debug) cuando hay permisos forzados, con botón
    "Restaurar permisos reales" (nuevo `GodViewModel.clearOverride()`).
  **Lección:** una herramienta de desarrollo que altera permisos debe ser imposible de
  confundir con el comportamiento real: gatearla en compilación (no solo ocultarla),
  purgar su estado al cambiar de variante, y avisar en pantalla mientras esté activa.
  Un estado de debug persistido en DataStore sobrevive a reinstalaciones y parece un bug
  de producto (→ checklist #14, nuevo).

### 2026-08-07
- **#9** run `84484175476` · commit `11e006a` · `compileDebugKotlin` FAILED ·
  `Unresolved reference 'collectAsState'` (SettingsScreen.kt:590) y, en cascada,
  `Unresolved reference 'forceAccount' / 'forceSubscription'` (591).
  **Causa:** al añadir el banner de God mode escribí `godViewModel.ui.collectAsState()`,
  pero SettingsScreen importa `androidx.lifecycle.compose.collectAsStateWithLifecycle`
  (y lo usa en sus otras 6 lecturas), no `androidx.compose.runtime.collectAsState`. Los
  errores de forceAccount/forceSubscription eran consecuencia: sin resolver el delegate,
  el tipo de godUi quedaba desconocido.
  **Fallo de MI verificación previa:** comprobé con `grep -c "import.*collectAsState"`,
  que hace match PARCIAL y cuenta `collectAsStateWithLifecycle` como si fuera el import
  buscado -> falso positivo. El gate no detectó nada.
  **Fix:** usar `collectAsStateWithLifecycle`, el patrón del propio fichero.
  **Lección:** los chequeos de imports deben anclar el final de línea
  (`^import .*\.SIMBOLO$`), nunca `grep "import.*SIMBOLO"`: los nombres de la API de
  Compose son prefijos unos de otros (collectAsState / collectAsStateWithLifecycle,
  animateFloat / animateFloatAsState) y el match parcial da verde en falso. Regla
  general: al añadir código a un fichero, copiar el patrón que YA usa ese fichero en vez
  de introducir una API equivalente distinta (→ checklist #15, nuevo).
  Nota: el memo (Set A) prefiere `collectAsState`, pero SettingsScreen y BeamHubScreen
  usan `collectAsStateWithLifecycle` de forma consistente y funcionan; la coherencia
  por fichero manda sobre la preferencia global mientras no se migre todo a la vez.

### 2026-08-07 (rotación del Letrero LED — Opción A)
- **Hallazgo:** `MainActivity` declara `android:screenOrientation="portrait"` (con
  `tools:ignore="LockedOrientationActivity"` silenciando el aviso de lint), así que la app
  entera está bloqueada en vertical: girar el móvil NO hacía nada y la pista del modo LED
  ("gira el móvil para un letrero más ancho") prometía algo imposible.
- **Fix (Opción A, decisión de Pablo — rotación solo en el letrero):**
  · `LedBannerDisplay` pone `requestedOrientation = SCREEN_ORIENTATION_FULL_USER` mientras
    el display está activo y RESTAURA el valor previo en `onDispose`. setRequestedOrientation
    sobrescribe el manifest en runtime. El resto de la UI (no diseñada para landscape) sigue
    en vertical. La Opción B (desbloquear toda la app) queda pendiente y exigiría QA de todas
    las pantallas en apaisado; varias dimensionan con `screenHeightDp`.
  · **Bug derivado que habría empeorado el fix:** la Activity no declaraba `configChanges`,
    así que al girar Android la RECREABA; el estado `running` del display es un `remember`
    normal -> el usuario habría sido EXPULSADO del letrero de vuelta al editor en cuanto
    girase. Añadido
    `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"`:
    Compose maneja el cambio de configuración sin recrear y el estado sobrevive.
    (Se prefiere esto a `rememberSaveable` para `running`, que por checklist #12 podría
    dejar al usuario reabriendo directamente en el display.)
  · pista reformulada ("ya en marcha, gira el móvil…") porque se lee en el editor, que
    sigue en vertical.
  **Lección:** antes de prometer un gesto en la UI (girar, deslizar), verificar que el
  manifest/plataforma lo permite; y al habilitar rotación, comprobar SIEMPRE si la Activity
  se recrea, porque cualquier estado en `remember` se pierde (→ checklist #16, nuevo).

### 2026-08-07 (Fase 1a — toolchain de build)
- **Bump de toolchain, primer empujón de la migración a API 36** (deadline Play: 31 ago 2026).
  `agp 8.7.3 → 8.12.0` · `kotlin 2.1.10 → 2.2.10` · `ksp → 2.2.10-2.0.2` ·
  `gradle 8.9 → 8.13` · `compileSdk 35 → 36`. v0.7.0 (versionCode 13).
- **Decisión: AGP 8.12, NO 9.2.** La rama parkeada `chore/toolchain-modernization-a1` usó
  exactamente esta combinación y está marcada como verificada compilando SOBRE ESTE CÓDIGO;
  la rama que saltaba a AGP 9.2 (`restyle/android17-expressive`) lleva en su propio commit
  el aviso "NO mergear" y caveats abiertos (`android.newDsl=false` temporal). Con 24 días de
  plazo se prioriza la combinación verificada. AGP 9 queda para después de publicar.
- **`targetSdk` se queda en 35 a propósito.** Subir `compileSdk` solo permite compilar contra
  las APIs nuevas (sin cambio de comportamiento); subir `targetSdk` a 36 activa los cambios de
  Android 16 (prohibición de bloquear orientación en pantallas ≥600dp, fin del opt-out de
  edge-to-edge) y eso exige QA en apaisado de todas las pantallas → va en la Fase 3.
- **`kotlinOptions` migrado a `kotlin { compilerOptions { jvmTarget } }`.** Estaba deprecado en
  AGP 8.x y desaparece en AGP 9; migrarlo ahora evita repetirlo. Requiere el tipo
  `org.jetbrains.kotlin.gradle.dsl.JvmTarget` (fully-qualified para no tocar imports).
- **KSP, no kapt.** Comprobado: Hilt usa `ksp(libs.hilt.compiler)`. Esto elimina el obstáculo
  mayor de AGP 9 (kapt es incompatible con el Kotlin integrado de AGP 9).
- **Compose BOM deliberadamente SIN tocar** en este empujón (sigue en 2024.09.03). Va en la
  Fase 1b para aislar fallos: el salto a 2026.06.00 (material3 1.3 → 1.4) traerá deprecaciones
  de API en el código de UI, y mezclarlo con el cambio de compilador haría imposible saber
  qué rompió qué.
  **Lección:** en una migración de toolchain, separar el cambio de COMPILADOR del cambio de
  LIBRERÍAS en pushes distintos; si van juntos, un fallo no dice cuál de los dos lo causó
  (→ checklist #17, nuevo).

### 2026-08-07 (Fase 1b — Compose BOM)
- **Fase 1a VERDE** (run `84529648601`, `a3bbb0f`): BUILD SUCCESSFUL en 4m49s con AGP 8.12 /
  Kotlin 2.2.10 / KSP 2.2.10-2.0.2 / Gradle 8.13 / compileSdk 36. Confirma que el salto de
  COMPILADOR está limpio: cualquier fallo de 1b será de librerías, que era el objetivo de
  separar las fases.
  Avisos benignos observados: infraestructura del CI (Node 20 deprecado, `setup-java@v4`
  deprecado → migrar a v5 en un commit aparte) y un aviso NUEVO de Kotlin 2.2 en ~10 ficheros
  ("annotation applied to the value parameter only, but in the future also to field") por
  anotaciones tipo `@StringRes`/`@DrawableRes` en parámetros. Hoy es warning; conviene fijar
  el use-site target antes de que Kotlin lo convierta en error.
- **Fase 1b:** `composeBom 2024.09.03 → 2026.06.00` (Material3 1.3 → 1.4). v0.7.1 (vc 14).
  Cambio de UNA línea a propósito: `lifecycle 2.8.6`, `activityCompose 1.9.2` y
  `navigation 2.8.0` se dejan intactos porque la rama parkeada verificada compilando usaba
  EXACTAMENTE esas versiones junto a la BOM 2026.06.00.
- **Comprobación previa de APIs que rompen entre M3 1.3 y 1.4** (hecha ANTES de empujar):
  · `LinearProgressIndicator` ya usa la firma con lambda `progress = { }` (la de `Float` está
    deprecada) — OK.
  · sin `rememberRipple` (deprecado → `ripple()`) — OK.
  · sin `Divider` antiguo; el código ya usa `HorizontalDivider` — OK.
  · `SegmentedButtonDefaults.itemShape(index, count)` sin cambios de firma — OK.
  **Lección:** antes de un salto grande de librería, hacer un barrido dirigido de las APIs que
  se sabe que cambian, en vez de empujar y esperar al CI. Aquí salió limpio, pero el barrido
  cuesta un minuto y habría ahorrado un ciclo entero si hubiera encontrado algo
  (→ checklist #18, nuevo).

### 2026-08-11 (Fase 2a — desbloqueo de orientación)
- **Fase 1b VERDE** (run `85299782049`, `38fc2b9`): BUILD SUCCESSFUL en 2m41s con Compose BOM
  2026.06.00 / Material3 1.4. Única deprecación nueva: `createComposeRule` → `junit4.v2` en los
  dos tests Robolectric (excluidos del CI, no urgente). El barrido previo de APIs acertó: no
  hubo ninguna sorpresa. **Fase 1 cerrada.**
- **Fase 2a:** quitado `android:screenOrientation="portrait"` del manifest (y su
  `tools:ignore="LockedOrientationActivity"`). Requisito de API 36: en pantallas ≥600dp el
  bloqueo se ignora, así que había que diseñar para apaisado ANTES de subir targetSdk.
- **Auditoría previa de qué se rompía al girar** (leyendo el código, no probando):
  · `OnboardingScreen` — ÚNICA pantalla sin `verticalScroll`: en apaisado los botones quedaban
    fuera de pantalla, sin forma de alcanzarlos. **Fallo real, no cosmético.**
  · `BeamHubScreen` — `sheetMaxHeight = screenHeightDp * 0.42f` y `orbSize` derivado de la
    altura: con ~360dp de alto la hoja quedaba en ~150dp y el orbe en su mínimo de 180dp,
    solapándose. Inservible aunque no crashee.
  · `SettingsScreen`, `AuthScreen`, `SoundAlertScreen` — sobreviven (ya tienen scroll); el
    problema era de legibilidad: líneas de 90+ caracteres.
- **Aplicado en este commit:**
  · Onboarding reorganizado a DOS PANELES cuando `screenHeightDp < 480`: ilustración a la
    izquierda, textos + indicador + botones a la derecha. Piezas extraídas a composables
    (`ObIllustration`, `ObTexts`, `ObIndicator`, `ObPrimaryButton`, `ObBackRow`) y REUTILIZADAS
    en ambas disposiciones: es la misma composición reordenada, no dos layouts paralelos.
  · Ajustes: contenido acotado a `widthIn(max = 600.dp)` centrado. En vertical no cambia nada.
  · `LedBannerScreen`: el fallback de restauración pasa de `SCREEN_ORIENTATION_PORTRAIT` a
    `UNSPECIFIED`, porque ya no hay bloqueo al que volver.
- **PENDIENTE (Fase 2b):** el Beam Hub a dos paneles. Su hoja vive en el `bottomBar` del
  `Scaffold`, así que pasarla a panel lateral es reestructurar un fichero de ~900 líneas.
  Se deja para un commit aislado (checklist #17: separar cambios para poder atribuir fallos).
  `targetSdk` sigue en 35 hasta cerrar 2b y hacer el QA en dispositivo.

### 2026-08-11 (Fase 2b — Beam Hub a dos paneles)
- **Problema:** el Beam Hub repartía el espacio por ALTURA (`sheetMaxHeight = alto * 0.42f`,
  `orbSize = alto * 0.27f` acotado a 180..240dp). En apaisado (~360dp de alto) la hoja quedaba
  en ~150dp y el orbe en su mínimo de 180dp: solapados e inservibles. No crasheaba, pero la
  pantalla principal quedaba inutilizable, y con API 36 el apaisado deja de ser evitable.
- **Solución sin duplicar layouts:** la hoja de controles se extrae a una **lambda composable
  local** `val controlSheet: @Composable (side: Boolean) -> Unit`. Definirla como lambda dentro
  del composable (en vez de función top-level) le deja CAPTURAR el estado local — `state`,
  `hazeState`, `viewModel`, `infoVisible`, `sheetContainer`… — sin pasar veinte parámetros.
  Se invoca en dos sitios:
  · vertical → `bottomBar = { if (!wideLayout) controlSheet(false) }`
  · apaisado → panel lateral derecho `Box(Modifier.weight(0.44f).fillMaxHeight())`
  El flag `side` solo altera forma (esquinas izquierdas vs superiores), cómo se estira
  (`fillMaxHeight` vs `fillMaxWidth` + tope de altura) y si aplica `navigationBarsPadding`
  (que en panel lateral no tiene sentido).
- El contenido se envuelve en un `Row`; el orbe pasa a `alto * 0.46f` acotado 120..200dp cuando
  `wideLayout`, porque ahora comparte ancho en vez de competir por altura.
- `wideLayout = screenHeightDp < 480`: el criterio es la ALTURA disponible, no la orientación
  nominal — así también cubre ventanas pequeñas en multiventana y plegables.
  **Lección:** cuando un layout deba existir en dos disposiciones, extraer el bloque a una
  lambda composable local y parametrizar SOLO lo que cambia; duplicar el bloque garantiza que
  las dos copias diverjan con el tiempo (→ checklist #19, nuevo).

### 2026-08-11 (Fase 2c — correcciones del apaisado tras QA en dispositivo)
- **Fase 2b verde en CI pero con 4 defectos visibles en dispositivo** (captura de Pablo).
  El patrón de dos paneles funcionaba, pero:
  · **Pesos mal calculados.** `Column(weight(1f))` + `Box(weight(0.44f))` NO reparte 56/44:
    Compose normaliza sobre la SUMA (1.44), así que daba 69%/31%. El carrusel de modos, más
    ancho que su panel, quedaba parcialmente OCULTO bajo la hoja. Corregido a 0.58f/0.42f.
    **Trampa a recordar: los weight son proporciones sobre el total, no fracciones de 1.**
  · **Orbe desbordado.** `alto * 0.46f` acotado a 120..200dp: con ~393dp de alto útil, entre
    rail (~90dp), orbe (200dp) y píldora de estado (~36dp) no cabía, y el orbe se recortaba
    por abajo llevándose la píldora. Reajustado a `alto * 0.34f` acotado 96..150dp.
  · **`navigationBarsPadding` mal quitado del panel lateral.** Se retiró en 2b pensando que
    "en panel lateral no aplica", pero en APAISADO la barra del sistema se coloca precisamente
    en ese lateral. Restaurado siempre, y añadido `displayCutoutPadding()` en modo panel
    porque el recorte de cámara también cae de lado.
  **Lección:** los insets no dependen del sitio del componente sino de la ORIENTACIÓN del
  dispositivo; razonar "esto va al lado, luego no necesita padding de barra" es exactamente al
  revés en landscape (→ checklist #20, nuevo).
  **Lección 2:** un build verde no valida un layout. Los cuatro defectos compilaban
  perfectamente; solo la captura en dispositivo los reveló.

### 2026-08-11 (Fase 2d — ancho máximo en el resto de pantallas)
- **QA de Pablo en pantalla grande VERTICAL** (tablet/plegable, capturas de Alerta Sonora,
  Ajustes, Letrero LED y Acceder en v0.7.3). Descubre un problema DISTINTO al del apaisado:
  en superficies anchas las tarjetas y campos se estiran a todo el ancho, separando cada
  etiqueta de su control (los switches de Alerta Sonora quedaban a un palmo de su texto) y
  dejando campos de correo/contraseña larguísimos para su contenido.
- **Causa:** el tope `widthIn(max = 600.dp)` de la Fase 2a se aplicó SOLO a `SettingsScreen`.
  El resto de pantallas con scroll (`SoundAlertScreen`, `AuthScreen`, `LedBannerScreen`)
  seguían a ancho completo.
- **Fix:** mismo patrón —`Box(fillMaxSize, TopCenter)` envolviendo un `Column` con
  `widthIn(max = 600.dp)`— aplicado a las tres. En móvil vertical no cambia nada: `widthIn`
  solo actúa cuando sobra ancho.
  **Lección:** al introducir una regla de layout adaptativo, aplicarla de una vez a TODAS las
  pantallas del mismo tipo; hacerlo en una sola deja el resto divergiendo y el fallo no
  aparece hasta que alguien prueba en una pantalla grande (→ checklist #21, nuevo).
- **Validado de paso:** el Letrero LED rasteriza emojis correctamente en la rejilla de puntos
  (captura con "😄 Hello" desplazándose). Confirmada esa feature en dispositivo.

### 2026-08-11 (Fase 2e — cabecera adaptativa en Acceder)
- **QA de Pablo: `AuthScreen` en apaisado.** La cabecera de marca (icono 76dp + titulo
  `headlineMedium` + subtitulo) consumia practicamente todo el alto util (~393dp), dejando el
  formulario BAJO EL PLIEGUE: habia que desplazarse solo para ver el campo de correo. En una
  pantalla de login eso es un fallo de usabilidad serio, no cosmetico.
- **Fix:** cabecera adaptativa con `compactHeight = screenHeightDp < 480` — icono 76→48dp,
  radio 24→16dp, glifo 40→26dp, titulo `headlineMedium`→`titleLarge`, subtitulo oculto (es
  redundante: el mismo texto ya aparece en Ajustes al invitar a crear cuenta) y espaciado del
  Column `lg`→`sm`. Con eso el formulario completo entra sin scroll.
- **Decision deliberada:** NO se aplica el mismo adelgazamiento a la `DisclosureCard` de
  `SoundAlertScreen`, aunque ocupe espacio parecido: contiene el aviso legal ("no es un
  sistema de seguridad, no sustituye a un detector homologado"). Ese texto debe verse siempre
  y en cualquier orientacion.
  **Leccion:** al recuperar espacio vertical en pantallas compactas, distinguir contenido
  DECORATIVO (cabeceras de marca, ilustraciones) de contenido OBLIGATORIO (avisos legales,
  disclaimers de seguridad). Lo primero se adelgaza u oculta; lo segundo nunca
  (→ checklist #22, nuevo).

### 2026-08-11 (Fase 2f — el tope de ancho NUNCA se aplicó + texto partido en el panel)
- **BUG DE MODIFICADORES (afectaba a 4 pantallas desde la Fase 2a).** El tope de 600dp se
  escribió como `Modifier.fillMaxSize().widthIn(max = 600.dp)`. **El orden lo anula:**
  `fillMaxSize()` fija minWidth = maxWidth = ancho del padre; `widthIn` no puede bajar el
  maxWidth por debajo del minWidth ya impuesto, así que Compose lo descarta en silencio.
  Resultado: Ajustes, Alerta Sonora, Acceder y Letrero LED seguían a ancho COMPLETO en
  tablet, y las Fases 2a y 2d no hicieron nada visible pese a compilar y pasar el CI.
  **Fix:** `Modifier.widthIn(max = 600.dp).fillMaxHeight()` — primero el tope, y solo el alto
  se estira.
  **Lección:** en Compose el orden de los modificadores de tamaño es semántico, no
  cosmético: un `fillMaxX` antes de un `widthIn`/`heightIn` lo cancela. Ante un tope que
  "no se ve", sospechar del orden antes que del valor (→ checklist #23, nuevo).
- **Texto partido por sílabas en el panel lateral.** Los presets de Baliza usaban
  `Row` + `FilledTonalButton(Modifier.weight(1f))`: repartir el ancho a partes iguales en un
  panel estrecho dejaba cada botón por debajo del ancho de su etiqueta, y "Localización"
  se rompía en "Loc / aliz / aci / ón" (4 líneas). Cambiado a `FlowRow` sin `weight`: cada
  botón toma su ancho natural y baja de línea entero si no cabe. `maxLines = 1` como red.
  **Lección:** `weight(1f)` reparte espacio SIN mirar el contenido; para etiquetas de texto
  variable (i18n incluida) usar `FlowRow` o anchos naturales (→ checklist #24, nuevo).

### 2026-08-11 (Fase 2g — píldora de estado fuera de pantalla en apaisado)
- **Síntoma:** en el Beam Hub girado, la `StatusPill` ("Toca para encender · Continuo") no
  aparecía nunca bajo el orbe (confirmado en 3 capturas de Pablo).
- **Causa:** el Column de contenido centraba con dos `Spacer(Modifier.weight(1f))`, uno antes
  del orbe y otro después de la píldora. Los pesos reparten el espacio SOBRANTE; si el
  contenido intrínseco ya llena el alto, los pesos valen 0 y el ultimo hijo — la píldora —
  simplemente queda fuera del viewport, SIN scroll y sin ningún aviso. Además `orbDiameter`
  es el diámetro EXTERIOR (incluye la corona: el orbe visible es 176/252 del valor), así que
  150dp reservaban más alto del que aparentaba el círculo.
- **Fix:** en `wideLayout` se sustituye el centrado por pesos por
  `verticalArrangement = Arrangement.Center` (centra el bloque completo y es estable ante
  desbordes) y los dos Spacer flexibles se aplican solo en vertical. Orbe reajustado a
  `alto * 0.30f` acotado 88..128dp.
  **Lección:** centrar con `Spacer(weight)` es frágil en pantallas cortas: ante desborde el
  contenido no se recorta de forma visible, se EXPULSA silenciosamente. Para bloques que
  deben verse enteros, usar `Arrangement.Center` (→ checklist #25, nuevo).
  **Lección 2:** comprobar si una medida es diámetro interior o exterior antes de usarla para
  calcular espacio disponible.
