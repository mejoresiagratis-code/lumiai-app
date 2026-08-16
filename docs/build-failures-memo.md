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

### 2026-08-11 (Fase 2h — modo Pantalla en apaisado)
- **Confirmado OK:** el tope de 600dp ya funciona tras la Fase 2f (captura de Acceder en
  apaisado con el contenido acotado y centrado). El bug de orden de modificadores queda cerrado.
- **Nuevo problema, en un fichero que no se habia tocado:** el panel de ajustes de
  `ScreenLight` (modo Pantalla / Modo Intimo) no tenia NI tope de altura NI scroll interno.
  Crecia hasta caber todo su contenido (Luz de color + 4 atmosferas + Animacion + Brillo +
  Temporizador), lo que en apaisado (~393dp de alto) significaba TAPAR la pantalla entera,
  incluido el aviso "toca fuera para apagar" — que es la unica pista de como salir del modo.
- **Fix:** `heightIn(max = alto * 0.72f)` + `verticalScroll` en el Column del panel, y
  `widthIn(max = 640.dp)` en el Surface para no estirar los controles en superficies anchas.
  **Leccion:** cualquier hoja/panel cuyo contenido pueda crecer necesita tope de altura Y
  scroll interno desde el primer dia; sin ellos el fallo no aparece en vertical y se destapa
  entero al girar. Al auditar layouts para apaisado hay que revisar TODAS las hojas del
  proyecto, no solo la de la pantalla principal (→ checklist #26, nuevo).

### 2026-08-11 (Fase 2i — CAUSA REAL de la píldora fuera de pantalla)
- **Tres intentos fallidos antes de encontrarla** (2c reduciendo el orbe, 2g cambiando a
  `Arrangement.Center`, 2h tocando otro fichero). En los tres se supuso la causa en vez de
  leer el código. Pablo lo señaló: hay que CONFIRMAR, no suponer.
- **Causa real:** `PowerOrb` se dimensiona con **`Modifier.requiredSize(orbDiameter)`**.
  `requiredSize` IGNORA las restricciones del padre: el orbe mide siempre ese valor exacto
  aunque el Column no tenga sitio. Por eso ninguna de las medidas anteriores funcionó:
  · reducir el porcentaje del orbe achicaba el círculo pero no resolvía el desbordamiento
    cuando la cuenta no salía;
  · `Arrangement.Center` no ayuda porque no hay "sobrante que centrar", hay un hijo que se
    NIEGA a comprimirse y empuja al siguiente (la píldora) fuera del viewport.
- **Fix:** en apaisado el tamaño del orbe se calcula RESTANDO lo que ocupan la barra
  superior, el carrusel y la píldora, en vez de ser un porcentaje del alto total:
  `available = alto - rail(104) - pill(64) - topBar(64)`, acotado 72..132dp. Además el
  padding superior de la píldora baja de `lg` a `sm` en apaisado.
  Verificado con los números reales del dispositivo de Pablo (S26 Ultra apaisado, ~393dp):
  64+104+132+48 = 348dp → 45dp de holgura.
  **Lección:** `requiredSize` / `requiredWidth` / `requiredHeight` rompen el contrato de
  restricciones de Compose. Ante un hijo que "no cabe y no se encoge", buscar `required*`
  antes de tocar arrangements o porcentajes (→ checklist #27, nuevo).
  **Lección 2 (proceso):** cuando un fix no funciona a la primera, NO reintentar con otra
  suposición: leer el layout del componente implicado. Tres iteraciones perdidas por no
  abrir `PowerOrb`.

### 2026-08-11 (Fase 2j — auditoría sistemática de vistas a pantalla completa)
Revisión buscando PATRONES en todo el proyecto, no pantalla por pantalla:
- **`required*`:** solo un uso en todo el código (`PowerOrb.requiredSize`), ya tratado en 2i.
  Confirmado que no hay más componentes que ignoren las restricciones del padre.
- **Pantallas sin scroll:** todas tienen `verticalScroll`/`LazyColumn` salvo `OnboardingScreen`,
  que se resolvió con dos paneles en 2a (decisión deliberada: añadir scroll escondería el
  botón principal).
- **`ScreenBeacon`:** destello blanco a pantalla completa sin contenido — sin problema.
- **INSETS, los fallos reales encontrados:**
  · `ScreenLight`, texto "toca fuera para apagar": usaba `padding(top = xxl)` FIJO. Con
    edge-to-edge (API 36) el texto queda bajo la barra de estado. → `statusBarsPadding()` +
    `displayCutoutPadding()`.
  · `ScreenLight`, botón de candado en `Alignment.TopEnd`: en APAISADO la barra de navegación
    se coloca en el lateral derecho, exactamente donde vive ese botón → quedaría **debajo e
    intocable**. → `systemBarsPadding()` + `displayCutoutPadding()`.
  · `ScreenLight`, panel: tenía `navigationBarsPadding` pero no recorte de cámara. → añadido.
  · `LedBannerDisplay`: el fondo negro debe ir a sangre (es un letrero), pero la REJILLA no:
    las letras que pasan bajo el notch se veían mordidas. → `displayCutoutPadding()` solo al
    canvas, dejando el fondo full-bleed.
  · `BeamHubScreen`, contenedor de los dos paneles: sin protección de recorte; en apaisado el
    notch pasa al lateral izquierdo, donde arranca el carrusel. → añadido.
  **Lección:** en una vista a pantalla completa los insets se aplican al CONTENIDO, nunca al
  contenedor raíz — el fondo debe seguir cubriendo toda la pantalla (más aún si el fondo ES
  la función: una linterna de pantalla o un letrero). Y todo elemento anclado a un borde
  (`TopEnd`, `BottomCenter`) es sospechoso: el borde que en vertical está libre, en apaisado
  es donde el sistema pone sus barras (→ checklist #28, nuevo).

### 2026-08-11 (Fase 2k — modo Pantalla a panel lateral + hoja de tableta acotada)
- **Decisión de diseño (mockup aprobado por Pablo):** la hoja INFERIOR de `ScreenLight` era
  el patrón equivocado en apaisado — compite por el eje escaso (el alto) y dejaba la luz en
  una franja del ~27% por mucho tope que se le pusiera (iteraciones 2h..2j fueron parches
  sobre un patrón que no podía funcionar). Patrón correcto: *supporting pane* (validado por
  la skill oficial `android/skills@adaptive`), implementado con la misma mecánica `side`
  del Beam Hub, sin migrar a Navigation 3 antes del deadline.
- **Aplicado:**
  · `ScreenLight`: con `compactHeight` (<480dp de alto) el panel pasa a LATERAL — 38% del
    ancho, alto completo, `CenterEnd`, esquinas `topStart+bottomStart` de 28dp. La luz
    conserva el fondo completo porque el panel se SUPERPONE (alpha 0xF0), no lo recorta.
    En vertical/tableta sigue siendo hoja inferior con su tope del 72% + scroll (red de 2h).
  · Aviso "toca fuera para apagar" y candado: `.padding(end = panelReserve)` DESPUÉS de
    `.align(...)` — el bloque medido incluye la reserva, así que quedan centrados/retirados
    sobre el ÁREA DE LUZ (62%), no sobre la pantalla entera ni bajo el panel.
  · Beam Hub, hoja de tableta: `widthIn(max = 640.dp)` (orden correcto, #23) + Box de
    centrado en el slot `bottomBar` — **el slot alinea al inicio**: sin el Box, una hoja
    acotada quedaría pegada a la izquierda en tableta, no centrada.
  **Lección:** cuando varios parches consecutivos mejoran un layout "un poco pero sigue mal",
  el patrón contenedor es el sospechoso, no las medidas: cambiar tope/scroll/porcentaje no
  arregla un contenedor anclado al eje equivocado (→ checklist #29, nuevo).

### 2026-08-11 (limpieza pre-Cabina — umbral único de altura compacta)
- **Auditoría de parches verticales con riesgo en apaisado** (petición de Pablo antes del
  rediseño Cabina). Resultado: `rememberSaveable` restantes son legítimos (paso del
  Onboarding y tarjeta expandida de Alerta Sonora DEBEN sobrevivir al giro); ningún
  `Spacer(weight)` sin condicionar; `requiredSize` solo en PowerOrb (documentado en #27,
  neutralizado por el dimensionado por resta).
- **Hallazgo real:** el umbral `screenHeightDp < 480` estaba definido CUATRO veces
  (BeamHub `wideLayout`, ScreenLight/Auth/Onboarding `compactHeight`). Hoy coinciden; el día
  que alguien ajuste uno, las pantallas cambiarían de disposición a alturas DISTINTAS — el
  clásico parche que diverge en silencio.
- **Fix:** `ui/util/WindowSize.kt` con `COMPACT_HEIGHT_THRESHOLD_DP = 480` +
  `isCompactHeight()` (@ReadOnlyComposable). Las cuatro pantallas leen la utilidad; imports
  de `LocalConfiguration` retirados donde quedaron sin uso (Auth, Onboarding). Cero cambio
  de comportamiento: mismo umbral, una sola fuente.
  **Lección:** toda condición de layout repetida en más de un fichero es deuda: extraerla a
  una fuente única ANTES de construir encima (la Cabina va a leer este mismo flag)
  (→ checklist #30, nuevo).

### 2026-08-11 (Fase 2l — afinado del VERTICAL antes de la Cabina, QA de Pablo)
- **Reporte con capturas (v0.8.3 vertical):** (a) la hoja medía distinto según el modo —
  Baliza, con presets, crecía hasta EXPULSAR la píldora de estado: el mismo efecto del
  memo #27 pero en vertical, porque `heightIn(max)` deja que el contenido decida y
  `requiredSize` del orbe no cede; (b) hueco excesivo entre header y carrusel;
  (c) petición: hoja plegable tocando su parte superior.
- **Aplicado:**
  · **Altura FIJA de hoja** en vertical: `alto * 0.38f` para TODOS los modos, con scroll
    interno. La variable `sheetMaxHeight` (heightIn) desaparece.
  · **Plegado:** `sheetExpanded` en `rememberSaveable` (sobrevive al giro; default true),
    animado con `animateDpAsState` + `LumiMotion.emphasized()` a 72dp plegada. La zona
    clicable es TODA la franja superior (asa incluida), `onClickLabel` con string nueva
    `sheet_toggle_cd` (EN/ES en paridad, 274/274). Scroll deshabilitado al plegar para no
    scrollear dentro de 72dp. En panel lateral (`side`) el plegado se ignora.
  · **Orbe por RESTA también en vertical:** con la hoja fija el espacio es determinista:
    `alto − hoja − top(64) − rail(128) − píldora(72) − 24`, acotado 150..240dp.
    Verificado: S26 vertical ~890dp → 338+240+128+72+64 = 842 → 48dp de holgura.
  · **Carrusel pegado al header:** fuera el `padding(top = md)` del ModeRail.
- **El chequeo anclado (#15) volvió a pagar:** `rememberSaveable` quedó usado sin import y
  el gate lo cazó ANTES del CI (usos=1 import=0). Un ciclo ahorrado.
- Falso positivo conocido: el audit marca TODO por la palabra "TODOS" en un comentario.

### 2026-08-11 (Fase 2m — hoja plegada por defecto + barra de navegación oculta)
- **QA de Pablo (v0.8.4):** la hoja plegada quedaba DETRÁS del menú de navegación del sistema,
  y pidió (a) plegada por defecto siempre al abrir, aunque se cerrara desplegada, y (b)
  esconder el menú inferior.
- **Aplicado:**
  · `sheetExpanded` pasa a `remember { mutableStateOf(false) }`. **`remember` a propósito y
    no `rememberSaveable`:** con `configChanges` el giro NO recrea la Activity (el estado
    sobrevive igual), y `rememberSaveable` restauraría "desplegada" tras una muerte de
    proceso — justo lo contrario de lo pedido.
  · `MainActivity`: `WindowInsetsControllerCompat.hide(navigationBars())` con
    `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. La barra de estado no se toca. En apaisado
    esto además libera el lateral derecho (más ancho útil para el panel).
  · Cinturón y tirantes: si la barra reaparece (revelado transitorio u OEM que ignore el
    hide), su inset se SUMA a la altura de la hoja → la parte útil queda siempre por encima
    del menú. Con la barra oculta el inset es 0 y no altera la aritmética del orbe.
- **El guard de imports cayó en su propia trampa (#9 aplicada al ESCRITOR):** la comprobación
  `if 'import ...navigationBars' not in s` dio verde porque es PREFIJO de
  `navigationBarsPadding`, y el import real nunca se insertó (usos=1, import=0 → habría sido
  Unresolved reference en CI). El chequeo anclado posterior lo cazó en local.
  **Lección:** los guards de INSERCIÓN de imports deben usar regex anclada
  (`^import ...simbolo$`) igual que los de verificación; un `in` de substring sufre el mismo
  falso positivo que un grep sin anclar (→ checklist #31, nuevo).

### 2026-08-12 (REVERT completo del apaisado — decisión de producto de Pablo)
- **Decisión A:** revertir TODO el rango `38fc2b9..0372657` (Fases 2a–2m + limpieza) en un
  único commit de reversión, historial intacto. La app vuelve a estar BLOQUEADA EN VERTICAL.
- **Base legal/técnica de la decisión:** la norma de API 36 solo anula el bloqueo de
  orientación en pantallas de ancho mínimo ≥600dp (tablets/plegables abiertos). Un móvil
  como el S26 queda fuera: una app vertical con targetSdk 36 sigue siendo publicable y se
  comporta igual en móviles. En tablet el sistema ignorará el bloqueo (layout vertical
  estirado); mitigable con la propiedad de compatibilidad de Android 16 si llega el caso.
- **Se pierde a propósito** (reimplementable en una tarde con las lecciones aprendidas):
  orientación libre, paneles laterales (Beam Hub y Pantalla), topes de ancho, cabecera
  adaptativa de Acceder, dos paneles del Onboarding, insets de apaisado, hoja de altura
  fija + plegado (2l) y barra de navegación oculta (2m).
- **Se conserva:** el Letrero LED girando durante el display (anterior al rango, `25b5c2d`;
  su fallback vuelve correctamente a PORTRAIT con el revert) y este memo ÍNTEGRO — las
  lecciones #14–#31 son conocimiento, no layout, y siguen vigentes para cualquier trabajo
  futuro (varias aplican también en vertical: #23 orden de modificadores, #24 FlowRow,
  #25 Spacer(weight), #27 requiredSize).
- versionCode/Name avanzan a 0.9.0 (29) para que los dispositivos actualicen sin desinstalar.
- Si el apaisado vuelve algún día: el commit revertido documenta la implementación completa,
  y el roadmap Cabina + simulador HTML quedan entregados como referencia de diseño.

### 2026-08-12 (GATE — targetSdk 36)
- **`targetSdk 35 → 36`** (compileSdk ya estaba en 36 desde Fase 1a). v0.9.1 (versionCode 30).
  Requisito de Play para apps nuevas desde el 31-ago-2026; empujado con 19 días de colchón.
- **Barrido dirigido previo (lección #18) sobre app VERTICAL:**
  · Orientación: la cláusula de API 36 solo afecta a ≥600dp de ancho mínimo; el bloqueo
    vertical sigue efectivo en móviles (decisión A documentada en la entrada anterior).
  · Edge-to-edge sin opt-out: cero usos del opt-out legacy en el proyecto; `enableEdgeToEdge`
    activo desde antes; insets cubiertos por Scaffold (6 pantallas) + paddings explícitos (3).
  · Sin `LAYOUT_IN_DISPLAY_CUTOUT_MODE` manual que pudiera chocar.
- Pendiente de confirmar en CI + QA de humo de Pablo en dispositivo (0.9.1).

### 2026-08-12 (pulido de Ajustes — tipografía, ritmo y estructura; estilo actual, base para 1.1)
- **Petición de Pablo tras QA verde de 0.9.1:** afinar márgenes, interlineados, separaciones,
  tamaños y estructura de Ajustes; valorar submenús/expansiones. Sin cambiar de estilo.
- **Tipografía en las PRIMITIVAS (cascada a toda la pantalla):**
  · Cabeceras de sección: tracking 2sp→1.2sp + SemiBold + lineHeight 16 — 2sp hacía flotar
    las letras del overline.
  · `SettingsToggle`: la descripción bajaba de bodyMedium (14/20, 3 líneas y el switch
    flotando — visible en las capturas de Pablo) a bodySmall con interlínea 16sp; título a
    Medium; 2dp entre título y soporte.
  · `SettingsRow`: mismo tratamiento (título Medium, soporte 12/16).
- **Estructura:** el perfil de facturación se pliega tras una fila-cabecera con chevron
  animado (`animateFloatAsState` + `LumiMotion.emphasized`): se toca una vez en la vida y
  estorbaba a diario. `rememberSaveable(false)` — aquí SÍ saveable, a diferencia de la hoja
  del Beam Hub (2m): no hay preferencia del usuario que violar, solo continuidad de proceso.
- **Jerarquía de CTAs:** los botones de Alerta Sonora y Letrero LED pasan de `Button` a
  `FilledTonalButton`. Regla: el relleno primario queda RESERVADO para la conversión Pro
  (Acceso Pro / desbloqueo por anuncio); las herramientas son tonales.
- 2 strings nuevas EN/ES (276/276). Sin cambios de comportamiento fuera de Ajustes.

### 2026-08-12 (monetización — los diálogos de bloqueo reconocen el progreso de anuncios)
- **QA de Pablo:** el diálogo "Modo bloqueado" mostraba el copy genérico ("mira anuncios,
  2 = 1 h") aunque el usuario llevara ya 1/2 — mientras el drawer sí celebraba el progreso
  ("¡Ya casi! Llevas 1...") con CTA de urgencia. El momento de MÁXIMA intención (tocar un
  modo bloqueado) recibía el copy más flojo.
- **Fix:** flag único `lastAdPending = adsWatched >= adsPerGrant - 1 && !active` leído por
  las TRES superficies de diálogo (Beam Hub, Alerta Sonora, Letrero LED). Con el último
  anuncio pendiente: body → `pro_progress_one_left` y CTA → `pro_watch_ad_last`; si no,
  CTA `pro_watch_ad` ("Ver anuncio 1 de 2...") en vez del genérico "Ver anuncio". CERO
  strings nuevas: se reutilizan las del drawer, ya traducidas.
- **Dejado a propósito:** los diálogos de acento multicolor y de Música conservan el copy
  sin urgencia de anuncios — esas funciones solo se desbloquean con suscripción (regla de
  negocio), y prometer "1 anuncio más" ahí sería falso.

### 2026-08-12 (Q1+Q2 — firma de release, R8, Crashlytics, LeakCanary, StrictMode)
- **Corrección de memoria del proyecto:** el `FirebaseManager` reflexivo (memo #4) YA NO
  EXISTE en el código — cero `Class.forName`/`getMethod` en toda la app, y Firebase se reduce
  a Auth+Firestore vía BoM. El hallazgo ① de la auditoría se reclasifica: el riesgo no era
  reflexión sino que la build minificada JAMÁS se había compilado. Lección: los hallazgos de
  auditoría se verifican contra el código actual antes de actuar, no contra la memoria.
- **Firma:** `signingConfigs.release` lee 4 secretos de Actions (`LUMI_KEYSTORE_BASE64`,
  `_PASSWORD`, `LUMI_KEY_ALIAS`, `LUMI_KEY_PASSWORD`); el keystore se decodifica a
  `build/` (nunca al árbol de fuentes). SIN secretos cae a la firma de debug: el APK
  minificado es instalable para smoke pero imposible de publicar por accidente.
- **R8:** proguard-rules corto A PROPÓSITO (sin reflexión propia + consumer rules de las
  libs): atributos de línea para Crashlytics + strip de Log.v/d. Cada -keep innecesario es
  código sin optimizar.
- **Crashlytics:** plugin 3.0.5 + dep vía BoM. La subida del mapping la hace el plugin en
  release; el CI además lo guarda como artefacto 30 días. NOTA Data Safety (Q5): esto añade
  "registros de fallos" como dato recogido.
- **LeakCanary 2.14 (debugImplementation) + StrictMode** (thread+VM, detectAll+penaltyLog)
  antes de super.onCreate, solo debug.
- **CI:** job `release` solo en main: `assembleRelease` + artefactos APK y mapping. Los PRs
  siguen validando con debug (más rápido).
- Pendiente de Pablo: crear keystore y los 4 secretos (comando entregado en chat).

### 2026-08-13 (lección #32 — `java` está sombreado en Gradle Kotlin DSL)
- `assembleRelease` (primer intento de la historia) cayó en configure:
  `Unresolved reference: util` en `java.util.Base64.getDecoder()` dentro de
  `signingConfigs`. En un `.kts` de Gradle, el identificador `java` NO es el paquete:
  es el accessor generado de `JavaPluginExtension`, así que `java.util.*` inline se
  resuelve contra la extensión y no compila. Fix: `import java.util.Base64` en la
  cabecera del script (los imports van antes de `plugins {}`).
- Ojo aparte: este fallo tumbó TAMBIÉN el job de debug — un error de configure rompe
  todos los jobs del build, no solo el que usa esa rama de código.

### 2026-08-13 (lección #33 — base64 de secretos: filtrar espacios SIEMPRE)
- `Illegal base64 character 20` al decodificar `LUMI_KEYSTORE_BASE64`: la copia del churro
  desde el terminal de Codespaces en el móvil mete espacios en los puntos de quiebre de
  línea. Fix permanente en el build: `.filterNot { it.isWhitespace() }` antes de decode —
  cualquier futuro re-pegado del secreto queda inmunizado.
- **Ampliación (#33b):** el `2d` (guion) del segundo fallo venía del SILABEO del lector de
  texto de Pablo al partir la línea. Filtro definitivo: quedarse solo con el alfabeto
  base64 (`isLetterOrDigit() || +/=`) — cerrado por definición, inmune a cualquier visor.

### 2026-08-13 (lección #34 — primer R8 verde: dontwarn de javax.lang.model)
- `minifyReleaseWithR8` falló con `Missing class javax.lang.model.*` (AutoValue shaded
  dentro de una dependencia). Son clases de COMPILE-TIME inexistentes en Android: el fix
  correcto es `-dontwarn` (declarar ausencia esperada), NUNCA `-keep` (guardaría código
  muerto). R8 genera la lista exacta en `build/outputs/mapping/release/missing_rules.txt`.
- Cronología completa del primer assembleRelease de la historia del proyecto:
  #32 `java` sombreado en .kts → #33 base64 con espacios → #33b guiones de silabeo →
  #34 dontwarn. Cuatro capas, cuatro lecciones, cero presentes en debug.

### 2026-08-13 (Q3 — unit tests de negocio: huecos reales vs. huecos asumidos)
- **Auditoría real antes de escribir código:** lo que el roadmap llamaba "huecos" de Q3
  (RewardProgress, matriz Tier, Morse) ya tenían cobertura: `RewardProgressTest`,
  `ModeAvailabilityTest`, `MorseTest`, `TemporaryUnlockTest`, `BeatDetectorTest`, etc.
  La base es mejor de lo habitual para un indie.
- **Hueco real encontrado:** `lastAdPending` (v0.9.3, el flag que decide el copy de los
  diálogos de bloqueo) no tenía test. Es lógica pura — cero dependencias de Android/Compose
  — y gobierna la experiencia de monetización en el momento de máxima conversión.
  → Añadido `LastAdPendingTest` con 6 casos: 0/ADS, 1/ADS ("Ya casi"), umbral con/sin pro,
  pro activo con cualquier contador, y contador negativo.
- **Hueco LEGÍTIMAMENTE excluido:** `rasterize` del LED usa `android.graphics.Paint` y
  `Bitmap` — no testable en JVM puro. Va a Q6 (Roborazzi o screenshot instrumented).
- **Lección de proceso:** "audita antes de escribir" — el roadmap proyectado asumía huecos
  que ya no existían. Escribir tests repetidos habría sido ruido sin valor.

### 2026-08-13 (Q4 — lint gate, correccion de 12 errores LocalContext)
- 12 errores LocalContextGetResourceValueCall: context.getString dentro de lambdas onReward/
  onUnavailable que no corren en el scope de Compose. Fix: capturar las strings ANTES del
  callback en el scope Compose y cerrar sobre el valor. MusicFlashService: @Suppress
  documentado porque el getString de un Service Android es el patron correcto.
- Lint gate: abortOnError=true, warningsAsErrors=false. Silenciados con justificacion:
  GradleDependency/NewerVersionAvailable (actualizaciones programadas), UnusedResources
  (falsos positivos en Compose), AndroidGradlePluginVersion.
- Job quality ahora corre en cada push a main (antes solo manual).

### 2026-08-13 (lección #35 — la regla del lint era sobre la API, no sobre el scope)
- Tres iteraciones peleando contra `LocalContextGetResourceValueCall` moviendo
  `context.getString` de sitio (dentro del lambda → captura en el lambda → captura en el
  composable) y la regla seguía disparando. La regla NO es sobre dónde: PROHÍBE consultar
  recursos vía `LocalContext.current` en cualquier punto de un composable, porque ese valor
  no reacciona a cambios de idioma/configuración. La API correcta es `stringResource()`
  (composable, consciente de recomposición) capturada en scope composable y cerrada en el
  callback. **Leer la INTENCIÓN de la regla antes de la tercera iteración, no después.**
- Barrido del proyecto (petición de Pablo, "similares y futuros"):
  · 12/12 consultas en composables convertidas a stringResource (BeamHub 3, Settings 9).
  · Services/Repository (Music, SoundAlert, FirebaseAuth): getString via context es el
    patrón CORRECTO fuera de Compose — cero cambios, lint no los marca.
  · Cero autoreferencias `val x = x` en el proyecto.
  · FUTURO detectado: `LocalConfiguration.current.screenHeightDp` en BeamHub:288 (hoja al
    42%) dispara el warning ConfigurationScreenWidthHeight — API moderna: LocalWindowInfo.
    No bloquea (warning) y la app es vertical fija; migrar en Q6 junto al resto de Compose.

### 2026-08-13 (QA de release en S26 — 4 bugs de Pablo, auditoría + fixes + tests)
- **① Notificación parpadeando al ritmo del flash:** la NUESTRA es estable (TorchService,
  startForeground una vez, stopSelf solo al apagar — verificado); la que parpadea es la del
  SISTEMA de Samsung, que salta con cada setTorchMode(true) del pulso. No hay API para
  suprimirla. Mitigación: la nuestra pasa a ser LA completa — tap abre la app + botón
  "Apagar" (ACTION_STOP → repo.setOn(false); el colector hace el resto: un solo camino de
  apagado). Strings notif_action_off EN/ES (277/277).
- **② Dos notificaciones en Música:** el orbe arrancaba TorchService (notif "linterna en
  uso" sosteniendo awaitCancellation) ADEMÁS de MusicFlashService (notif propia). Fix:
  FlashViewModel.toggle deja MUSIC fuera del TorchService (Música es dueña de su sesión);
  MusicFlashService ya no apaga isOn al arrancar (el orbe encendido representa SU sesión)
  y lo apaga en onDestroy y en la parada por falta de permiso/LED (UI nunca miente).
- **③ Pro extensible infinitamente:** ver anuncios durante la hora activa reiniciaba el
  contador y ENCADENABA horas. Regla de producto fijada en DOMINIO: RecordRewardUseCase
  ignora anuncios con Pro activo (now inyectable para tests). UI: el botón de anuncio se
  OCULTA con Pro activo. 3 tests nuevos: activo-no-cuenta, caducado-vuelve-a-contar,
  umbral-con-activo-no-concede.
- **④ Crash-loop de Alerta Sonora:** startForeground de tipo MICROPHONE sin RECORD_AUDIO
  lanza SecurityException (API 34+); con START_STICKY el sistema reintentaba y la app moría
  en cada arranque hasta limpiar datos. Fix triple: permiso ANTES de startForeground +
  runCatching de cinturón + START_NOT_STICKY (la escucha solo revive por acción del
  usuario). MusicFlashService tenía el MISMO bug latente (startInForeground antes del
  check) — corregido en la misma pasada. Pantalla: doble guarda en onStart por si el
  permiso se revoca con la vista abierta.
- **Lección transversal:** los servicios de micrófono se auditan en pareja — un bug en uno
  suele existir en su gemelo.

### 2026-08-13 (lección #36 — verificar el tipo real, no asumir StateFlow)
- `repo.mode.value` no compiló: `FlashStateRepository.mode` es `Flow<FlashMode>`, no
  `StateFlow` — no expone `.value`. Fix: usar `uiState.value.mode`, el StateFlow ya
  combinado del propio ViewModel que sí lo trae. Lección: antes de escribir `.value`
  sobre cualquier propiedad, comprobar su tipo declarado — no asumir por el nombre.

### 2026-08-13 (bug de Pablo — "Continuar con Google" falla con mensaje genérico)
- **Causa raíz probable:** `app/google-services.json` tiene `certificate_hash` VACÍO en
  `android_client_info` — ninguna firma (ni debug, ni la release nueva de anoche) está
  registrada en el proyecto Firebase. Google Identity Services rechaza el idToken sin
  SHA-1 coincidente; el flujo cae al catch genérico.
- **Acción de Pablo (fuera del repo):** añadir el SHA-1 de debug Y de release en Firebase
  Console → Configuración → Android app → Añadir huella, y volver a descargar el JSON.
- **Fix de código, independiente de la causa de Firebase:** el error de Google compartía
  el mismo `AuthError.Unknown` → `auth_error_generic` que un fallo de email/contraseña,
  visualmente pegado al formulario — confundía qué se había tocado. Nuevo caso
  `AuthError.GoogleSignInFailed` con string propia (`auth_error_google`), propagado desde
  `AuthScreen`'s catch de `CredentialManager` y desde el `token == null`. Actualizado el
  `when` de `accountErrorMessage` en SettingsScreen (Kotlin habría marcado no-exhaustivo
  sin este caso — el compilador protegió aquí).

### 2026-08-13 (fix real de Google Sign-In — hash del keystore de release faltante)
- **Diagnóstico correcto tras corregir mi propio error de verificación (había mirado el
  campo `client_info.android_client_info.certificate_hash`, que es secundario/legacy;
  el que valida Google Sign-In es `oauth_client[].android_info.certificate_hash`):**
  Firebase solo tenía registrado el SHA-1 del keystore de DEBUG
  (`31:4B:32:98:8A:71:97:2D:6A:04:38:08:04:9B:54:8E:C4:7F:54:63`). El keystore de
  RELEASE, generado la noche del Q1, tiene un SHA-1 distinto por definición
  (`A1:0A:83:2F:A4:1C:0F:C5:C8:EC:38:C9:64:78:CB:67:B6:DF:79:9A`) y no estaba
  registrado — cualquier `idToken` de Google Sign-In pedido desde el APK de release
  era rechazado. Confirmado con la salida de `keytool -list -v` de Pablo.
- **Fix:** Pablo añadió la segunda huella en Firebase Console sin borrar la de debug
  (coexisten dos `oauth_client` de `client_type: 1`, uno por certificado). `google-services.json`
  actualizado en el repo. El `web client_id` (type 3, el que usa `AuthViewModel.webClientId`)
  no cambió — cero cambios de código necesarios.
- **Lección de proceso:** verificar el campo EXACTO que consume el flujo en cuestión, no
  el primero con nombre similar. `client_info.android_client_info.certificate_hash` está
  casi siempre vacío en los JSON reales — no es el que usa Credential Manager/Google Sign-In.

### 2026-08-13 (auditoría + 4 cambios de producto — acceso, arranque en frío, contador)
- **① SOS/Estrobo/Baliza/Morse (Tier.ADVANCED): solo cuenta en el pop-up.** Se retira el
  botón "ver 2 anuncios" de ESTOS diálogos — decisión de producto para no gastar
  impresiones en modos ligeros. La regla de acceso subyacente NO cambia: si el usuario
  ya tiene un desbloqueo temporal activo (ganado en Música/Alerta/LED), el modo no llega
  bloqueado en primer lugar (`AccessState.unlocks` sigue combinando cuenta O temporal).
  Nueva string `mode_locked_sign_in_only`.
- **② Música/Alerta Sonora/Letrero LED: invitado ve login + anuncio; con cuenta, anuncio
  + suscripción.** Música se reclasifica de `Tier.PRO` (estricto, sin anuncio) a `Tier.AI`
  (como Alerta/LED: suscripción O temporal) — cambio de regla de producto explícito de
  Pablo, documentado en la matriz de `Entitlements.kt`. Diálogo con dos ramas: invitado
  → primario "Iniciar sesión" + secundario "Ver anuncio" (sin cerrar la puerta del
  anuncio); con cuenta → primario "Ver anuncio" + secundario "Suscríbete" (como ya
  funcionaba Alerta/LED). `Tier.PRO` queda reservado, sin ningún modo actual usándolo.
  Nueva string `music_locked_body`. `BeamHubScreen`: diálogo reestructurado en `when`
  exhaustivo por tier (BASIC/PRO/ADVANCED/AI) con `watchAd` factorizado una vez.
- **③ Arranque en frío: siempre Continuo.** `DataStoreFlashStateRepository.mode` fuerza
  `FlashMode.CONTINUOUS` en la PRIMERA emisión de cada instancia — y al ser `@Singleton`
  de Hilt, nace exactamente una vez por proceso (app cerrada/actualizada = proceso nuevo).
  In-memory (`@Volatile`), sin bloquear el arranque con I/O. Reaperturas dentro del MISMO
  proceso (minimizar/maximizar) siguen respetando el modo elegido — solo un flip por vida
  del proceso. QA manual pendiente (no cubierto por unit test: requiere Robolectric):
  forzar-cerrar la app en SOS, reabrir, confirmar que entra en Continuo.
- **④ Contador de anuncios se reinicia en cada actualización de versión.** Regla PURA
  nueva: `ProProgressReset.shouldReset(lastVersionCode, currentVersionCode)` — testeada
  en JVM sin Android (4 casos: instalación inicial, misma versión, actualización,
  regresión). `RewardProgressRepository.resetIfVersionChanged` (nuevo método de interfaz)
  compara contra un versionCode persistido y resetea el contador en una única transacción
  atómica de DataStore. Disparado una vez por proceso desde `LumiAiApplication.onCreate`.
  NO toca el desbloqueo temporal ya activo — solo el contador hacia el PRÓXIMO premio.
- 4 strings nuevas EN/ES (281/281). 4 tests nuevos (ProProgressResetTest) + 2 tests
  existentes actualizados (Fake con el nuevo método; EntitlementsTest con Tier.AI).

### 2026-08-13 (EXPERIMENTAL — pulseOff: evitar el parpadeo de la notificación de Samsung)
- **Investigación (a petición de Pablo tras el bug de notificaciones):** ¿existe forma real
  de que el indicador del sistema no reaccione a cada pulso de SOS/Estrobo/Baliza/Morse?
  Hallazgo documentado en la API de Android 13+ (`CameraManager.TorchCallback`):
  `onTorchModeChanged` (encendida/apagada) y `onTorchStrengthLevelChanged` (solo
  intensidad, con la luz YA encendida) son callbacks DISTINTOS. `SystemUI` casi con
  toda seguridad escucha el primero para decidir cuándo mostrar su indicador, no el
  segundo — así que si el patrón nunca apaga de verdad la linterna (solo baja su
  intensidad al mínimo) durante los huecos, el indicador no debería re-dispararse.
- **Implementación:** nuevo método `TorchController.pulseOff()` junto a `turnOff()`
  existente. En dispositivos con intensidad variable (API 33+, `maxIntensityLevel > 1`,
  ya detectado por el slider existente) usa `turnOnTorchWithStrengthLevel(id, 1)` — el
  mínimo LEGAL de la API — en vez de `setTorchMode(false)`. Sin ese soporte, cae a un
  apagado real idéntico a `turnOff()`: CERO cambio de comportamiento en esos móviles.
  `FlashEngine` usa `pulseOff()` en los huecos DENTRO de un patrón activo (beacon,
  strobe, textMorse, morse) y reserva `turnOff()` (real) exclusivamente para el `finally`
  de fin de sesión. Mismo tratamiento en `SoundAlertService.flash()` y
  `MusicFlashService.pulse()` (mismo problema, misma causa).
- **RIESGO REAL, no cosmético — pendiente de validar en el S26 de Pablo:** el nivel
  mínimo de intensidad podría seguir siendo visible en oscuridad real (justo cuando
  importa un SOS), lo que difuminaría el contraste on/off del que depende la legibilidad
  del patrón. Si eso ocurre, hay que revertir `pulseOff()` a un alias de `turnOff()` —
  el cambio es reversible en un solo punto (la implementación de `Camera2TorchController`).
- Tests: `FlashEngineTest` actualizado (2 tests reescritos con la nueva semántica: la
  linterna NUNCA se apaga de verdad a mitad de patrón, solo al terminar la sesión) +
  1 test nuevo cubriendo el fallback en hardware sin intensidad variable.
  `FakeTorchController` espeja ambos caminos del controlador real.

### 2026-08-13 (notificación duplicada en Samsung — límites reales de la plataforma)
- **Petición de Pablo tras confirmar que pulseOff frenó el parpadeo:** ahora ve DOS
  notificaciones estables simultáneas — "Linterna encendida / Activado / Desactivar"
  (del SISTEMA, generada por Samsung, package ajeno) y "LumiAI / Linterna activa /
  Apagar" (la nuestra, `TorchService`). Pidió mostrar solo una según fabricante.
- **Dos restricciones duras de la plataforma, verificadas antes de escribir código:**
  · La notificación de Samsung la genera OTRO paquete del sistema — cero API para
    leerla, tocarla o suprimirla desde nuestra app. Imposible por diseño de Android.
  · Un servicio en primer plano DEBE mostrar una notificación (`startForeground`
    exige un `Notification` no nulo); "no mostrar nada" mata el servicio.
  · La importancia de un canal de notificación es INMUTABLE una vez creado — no se
    puede "bajar" en caliente para instalaciones que ya tengan el canal "torch".
- **Compromiso real implementado:** `ManufacturerInfo.isSamsung` (Build.MANUFACTURER)
  + canal NUEVO por fabricante en `TorchService` (`torch_samsung` en vez de `torch`,
  evita colisionar con el canal viejo de instalaciones previas). En Samsung nace en
  `IMPORTANCE_MIN`: sigue existiendo (obligatorio) pero sin icono en la barra de
  estado, "por debajo del pliegue" del panel — Samsung queda como la fuente visible
  práctica. En el resto de fabricantes sigue en `IMPORTANCE_LOW`, sin cambios.
- **Alcance deliberadamente limitado a `TorchService`:** Alerta Sonora y Música
  NO se tocan — sus notificaciones dicen "escuchando" / "parpadeando al ritmo de la
  música", información que Samsung NO muestra; no son redundantes, degradarlas
  perdería información real.
- **Efecto secundario menor, sin impacto funcional:** en dispositivos Samsung que ya
  tenían el canal "torch" antiguo (builds previas), quedará huérfano y visible en
  Ajustes > Apps > LumiAI > Notificaciones — solo ruido en esa pantalla del sistema,
  nunca en el uso normal de la app.

### 2026-08-13 (bug real de Pablo — "Desactivar" de Samsung no apagaba nuestra app)
- **Causa raíz:** el botón "Desactivar" de la notificación DEL SISTEMA de Samsung apaga
  el hardware directamente vía su propio mecanismo — nuestra app nunca se entera. Con
  SOS/Estrobo activo, el bucle de `FlashEngine` seguía llamando `torch.turnOn()` en cada
  pulso, ignorante de que algo externo había apagado la luz: la notificación de Samsung
  "revivía" en cada ciclo (`onTorchModeChanged` se volvía a disparar) y el botón de
  nuestra UI quedaba pillado en "encendido" para siempre — exactamente lo reportado.
- **Fix, aprovechando la arquitectura reactiva ya existente:** `TorchService` YA trataba
  `repo.isOn` como única fuente de verdad (el propio botón "Apagar" solo escribe ese
  flag; el colector existente para el motor y llama `stopSelf()` solo). Bastaba con
  detectar el apagado externo y escribir esa MISMA palanca.
- **Detección:** `Camera2TorchController` registra un `CameraManager.TorchCallback` una
  única vez (Singleton, vive con el proceso) y expone `externalOffEvents: Flow<Unit>`.
  El callback no dice QUIÉN apagó la luz — solo que se apagó — así que se filtra con una
  VENTANA DE TIEMPO (`SelfOffWindow`, regla pura con 4 tests JVM): si nuestro propio
  código llamó a `turnOff()` hace menos de 400ms, es nuestro y se descarta; si no, es
  externo. Una ventana es más robusta que una bandera booleana con set/clear porque el
  callback del sistema llega de forma ASÍNCRONA (Binder + Handler) en un hilo distinto
  al que hizo la llamada — una bandera podría limpiarse antes de que el callback la lea.
- **Caso sutil cubierto:** en hardware SIN intensidad variable, `pulseOff()` cae
  internamente a `turnOff()` real en cada hueco del patrón — eso TAMBIÉN marca
  `lastSelfOffAtMs`, así que esos dispositivos no generan falsos positivos de "apagado
  externo" en cada pulso normal de su propio patrón.
- **Aplicado a los 3 servicios que ciclan la linterna** (mismo problema, misma causa):
  `TorchService` (`repo.setOn(false)`), `MusicFlashService` y `SoundAlertService`
  (`stopSelf()`, que ya dispara su limpieza normal en `onDestroy`).
- Sin test end-to-end del flujo completo (requiere Robolectric, aún no montado en el
  proyecto — mismo hueco ya anotado para el arranque en frío). Cubierto en JVM puro: la
  regla `SelfOffWindow` que decide propio-vs-externo, con 4 casos.

### 2026-08-13 (revisión de estados de usuario — 3 reglas nuevas, una revierte trabajo previo)
- **① Probar Pro exige cuenta CON correo verificado.** Revierte deliberadamente el
  cambio de dos turnos atrás que dejaba a un invitado ver anuncios sin registrarse en
  Música/Alerta Sonora/LED — decisión de producto explícita de Pablo. Fuente única de
  verdad: `Entitlements.canTryProByAd() = hasAccount && isEmailVerified`, nuevo campo
  `isEmailVerified` poblado en `DefaultEntitlementRepository` desde `AuthUser`.
  `RewardedUnlockViewModel.watchAd()` lleva cinturón de seguridad (bloquea con
  `onUnavailable()` si `!canTryPro`, aunque la UI ya lo filtre). Diálogos de Música/
  Alerta/LED pasan de 2 a 3 vías: sin cuenta → solo "Iniciar sesión"; con cuenta sin
  verificar → aviso "Verifica tu correo" (dismiss-only en Ajustes: el flujo de reenvío
  ya vive ahí mismo, en Cuenta — un botón "de acción" ahí solo habría cerrado el
  diálogo, así que se quitó para no fingir); verificado → anuncio + suscripción, sin
  cambios. La tarjeta "Acceso Pro" del drawer refleja los mismos 3 estados.
- **② El desbloqueo temporal de Pro pierde persistencia entre reinicios de proceso.**
  Confirmado explícitamente por Pablo: cerrar la app del todo mata la prueba aunque
  queden minutos. Mismo patrón que el reinicio a Continuo de hace unas horas:
  `@Volatile isColdStart` en `DataStoreTemporaryUnlockRepository` — Singleton nace una
  vez por proceso, la primera lectura de `proUntilMillis` en cada proceso nuevo se
  fuerza a `0L` (expirado) sin bloquear el arranque con I/O. Logout también lo mata
  aunque no haya pasado la hora: `AccountViewModel.signOut()` llama
  `temporaryUnlock.clear()` antes de cerrar sesión.
- **③ Tema por defecto: Sistema, no Oscuro.** `AccentColor.BLUE` y `AccentStyle.VIVID`
  YA eran el default (verificado antes de tocar nada) — solo `ThemeMode` cambia de
  `DARK` a `SYSTEM`.
- 3 strings nuevas EN/ES (284/284). 5 tests nuevos (4 en `EntitlementsTest` para
  `canTryProByAd`, 1 en `EntitlementOverrideTest` para el nuevo campo tri-estado).

### 2026-08-13 (Alerta Sonora — dos bugs reales: crash sin manejar + UI que miente)
- **Petición de Pablo:** tras conceder el permiso de micrófono y tocar "Iniciar", la app
  se cierra y la escucha no arranca.
- **Auditado exhaustivamente antes de tocar código:** manifest (`foregroundServiceType`
  correcto en manifest Y runtime), permisos declarados, `MediaPipeSoundClassifier.start()`
  (ya envuelto en try/catch para `IllegalStateException`/`RuntimeException`, incluye
  `SecurityException` por herencia) — todo correcto. NO se pudo aislar la línea exacta sin
  un stack trace real de Pablo (pendiente si el bug reaparece: Crashlytics, activo desde
  Q2, lo capturaría). Dos problemas reales SÍ confirmados por lectura:
- **① Cinturón de seguridad ausente:** la corrutina que monta `SoundDetectionEngine` +
  `MediaPipeSoundClassifier` en `onCreate()` no tenía NINGÚN `try/catch` ni
  `CoroutineExceptionHandler`. Un `SupervisorJob` NO absorbe excepciones de sus hijos —
  solo evita que se cancelen entre sí. Cualquier fallo ahí dentro (config corrupta,
  MediaPipe, lo que fuera) tumbaba TODA LA APP, no solo el servicio. Envuelto en
  `runCatching { ... }.onFailure { stopSelf() }`: ahora degrada en vez de crashear.
- **② La UI mentía sobre si estaba escuchando.** `SoundAlertScreen` llevaba un
  `listening` LOCAL que se ponía a `true` en el mismo tap del botón, sin verificar NUNCA
  que el servicio arrancara de verdad. Si moría por cualquier razón (el propio crash de
  ①, u otra), el botón se quedaba pillado en "Parar" para siempre — coincide con lo
  reportado. Fix: `SoundAlertStateRepository` (in-memory, `@Singleton`, deliberadamente
  SIN DataStore — es estado de sesión puro, igual que la prueba de Pro de hace un rato)
  que el SERVICIO escribe (`true` tras `startInForeground()` exitoso, `false` en
  `onDestroy()`) y la pantalla observa vía el ViewModel. Con esto, si el servicio no
  llega a arrancar o muere, el botón vuelve solo a "Iniciar" — honesto en vez de mentir.

### 2026-08-13 (lección #37 — MediaPipe necesita reglas R8 explícitas, sus consumer-rules no bastan)
- **Bug de Pablo:** botón "Iniciar" de Alerta Sonora no cambia a "Parar" ni con
  permisos/categorías correctos. Encaja con el crash reportado antes de anoche: el
  `runCatching` defensivo añadido en v0.9.21 probablemente convirtió un crash visible
  en un fallo SILENCIOSO (el servicio se para solo, sin que se note por qué).
- **Causa confirmada por fuentes externas, no conjetura:** `proguard-rules.pro` tenía
  CERO reglas para MediaPipe (`tasks-audio`, usado por Alerta Sonora/YAMNet). MediaPipe
  lee sus mensajes protobuf (`GeneratedMessageLite`) por reflexión en runtime, y sus
  consumer-rules del AAR son INSUFICIENTES con R8 — confirmado con varios issues
  abiertos y sin resolver en el propio repo de Google (mediapipe#6138, #5141, #3509):
  el fallo típico es `Field xxx_ for Yyy not found` justo al llamar
  `createFromOptions()`. Esa excepción NO es `RuntimeException` en todos los casos
  (puede ser `ExceptionInInitializerError`), así que ni el catch interno de
  `MediaPipeSoundClassifier.start()` ni potencialmente el `runCatching` exterior la
  cubrían con seguridad según el tipo exacto.
- **Fix:** `-keep class com.google.mediapipe.** { *; }` + `-dontwarn` +
  `-keepclassmembers` para los campos de `GeneratedMessageLite`. Excepción deliberada
  a la filosofía de fichero corto — documentada con las fuentes que la justifican.
  Música NO usa MediaPipe (algoritmo propio de detección de golpes): sin exposición
  a este bug, confirmado antes de descartar tocar nada ahí.
- **Pregunta de producto de Pablo (permisos de Música al principio, como notificaciones):
  respondida sin cambio de código** — Música ya pide RECORD_AUDIO en el momento de
  usarlo (tocar el orbe en ese modo), no al abrir la app. Es el patrón que Android
  recomienda oficialmente: pedir permisos en contexto reduce denegaciones reflejas
  frente a pedirlos de golpe al arrancar sin que el usuario entienda para qué.

### 2026-08-14 (lección #38 — diagnóstico en el dispositivo: se acabó adivinar a ciegas)
- **Mandato de Pablo:** encontrar qué mata SoundAlertService al pulsar Iniciar (el botón
  cambia y revierte AL INSTANTE) y arreglarlo de una vez sin romper nada. find-skills
  ejecutado según protocolo: nada del ecosistema supera el listón de calidad para esto
  (colecciones genéricas de prompts) — auditoría propia.
- **Hechos verificados, no supuestos:** yamnet.tflite EXISTE (4,1 MB en assets); CI verde
  en v0.9.22; el síntoma "cambia y revierte" acota los sospechosos a EXACTAMENTE dos
  caminos (los únicos stopSelf posteriores a setListening(true)): el colector de
  apagado externo y el onFailure del clasificador.
- **Corrección de diseño que elimina al sospechoso ①:** el colector de apagado externo
  hacía stopSelf() — matar la ESCUCHA entera porque el sistema apagó la LINTERNA era
  desproporcionado (apagar la luz ≠ querer dejar de escuchar) y encima era un candidato
  a matar el servicio por eventos espurios. Ahora solo cancela flashJob (el destello en
  curso). Con esto, el ÚNICO camino que puede matar un servicio ya arrancado es el fallo
  del clasificador.
- **Observabilidad que cierra el diagnóstico:** `stopReason` en SoundAlertStateRepository
  — cada muerte del servicio registra su motivo EXACTO (permiso / startForeground /
  clasificador, con clase y mensaje de la excepción) y la pantalla lo muestra bajo el
  botón Iniciar en color de error. El próximo test de Pablo YA NO es una adivinanza: si
  revierte, el móvil dice literalmente qué lo mató. Un arranque exitoso limpia el motivo.
- **Refuerzo R8 adicional:** `-keep com.google.protobuf.**` — mediapipe#6138 muestra
  clases protobuf ofuscadas ("j3.d") en el crash; protobuf-lite es pequeño, mantenerlo
  entero elimina esa variable de una vez.
- **Nota:** el fix de MediaPipe de v0.9.22 (lección #37) podría YA haber resuelto el
  fallo — Pablo aún no lo había probado al reportar. v0.9.23 lleva ambas cosas: el
  posible fix Y el diagnóstico que lo confirma o desmiente en una sola prueba.

### 2026-08-14 (lección #39 — el diagnóstico en pantalla funcionó A LA PRIMERA; causa raíz confirmada)
- **La captura de Pablo dice, textual:** "clasificador: ExceptionInInitializerError: null"
  en v0.9.23 release — la FIRMA EXACTA de mediapipe#6138 (muere el inicializador estático
  de TaskRunner bajo R8), Y con las reglas keep de mediapipe/protobuf de #37/#38 YA
  aplicadas. Conclusión dura: esos keeps son insuficientes para este caso.
- **Dos deudas del diagnóstico anterior, saldadas:** ① el "null" escondía la causa real —
  `ExceptionInInitializerError.cause` lleva la excepción que lanzó el `<clinit>` y no la
  imprimíamos. `describeThrowable()` ahora recorre la cadena de causas (3 niveles,
  " <- " como separador). ② Causa raíz probable según la traza de #6138 ("no caller
  found on the stack for: j3.d"): FLOGGER, el logger de Google que MediaPipe usa — su
  detección de llamador camina la pila identificando sus propios frames POR NOMBRE, y
  la ofuscación/inlining de R8 los rompe. Keeps dirigidos añadidos (flogger, tensorflow).
- **Y la decisión de ingeniería: `-dontobfuscate` GLOBAL.** Es la ÚNICA solución
  CONFIRMADA como funcional en los issues del propio repo de MediaPipe (#5141, cita
  textual en el proguard). Trade-off aceptado conscientemente: sin renombrado de clases
  (algo más legible al reverse-engineering) pero SHRINKING y OPTIMIZACIÓN siguen activos
  — el tamaño apenas cambia. Con deadline de Play el 31-ago, fiabilidad > ofuscación.
  TAREA v1.1: quitar `-dontobfuscate`, probar en dispositivo si los keeps dirigidos
  bastan solos, y reactivar la ofuscación si es así.
- **Nota Crashlytics:** sin ofuscación no hay mapping que subir — los stacktraces llegan
  legibles de serie; el plugin de Crashlytics omite la subida sin quejarse.
- **Meta-lección:** invertir una iteración en observabilidad (stopReason en pantalla)
  convirtió un ciclo infinito de adivinanzas en UNA captura con el nombre exacto del
  asesino. Patrón a repetir ante cualquier fallo no reproducible en el sandbox.

### 2026-08-14 (lección #40 — detección muda: 3 bugs reales + observabilidad en vivo + allowBackup)
- **QA de Pablo:** escucha activa ✓ (fix #39 funcionó) pero no detecta nada ni notifica.
  Etiquetas YAMNet validadas una a una contra el mapa oficial del modelo: correctas.
  Canal ya en IMPORTANCE_HIGH. Tres bugs reales encontrados:
- **① Los sonidos TRANSITORIOS jamás podían disparar.** El motor exige 2 ventanas
  consecutivas (~1 s sostenido) sobre el umbral; un ladrido, un golpe o un timbre duran
  MENOS — cruzaban una ventana y morían en el debounce, matemáticamente incapaces de
  alertar. Nuevo flag `transientSound` en SoundCategory (TIMBRE, GOLPES_PUERTA, PERRO):
  1 ventana basta; el cooldown de 4 s sigue conteniendo el spam. 2 tests del motor
  migrados a DESPERTADOR (sostenido) + 1 test nuevo del transitorio.
- **② La notificación de detección MACHACABA la del servicio** — usaba el mismo
  NOTIF_ID=2 que la notificación del foreground, sustituyéndola en vez de crear una
  alerta nueva, y con setOngoing(true) (un evento puntual quedaba clavado como
  permanente). DETECTION_NOTIF_ID=4 propio + autoCancel.
- **③ POST_NOTIFICATIONS solo se pedía al acabar el onboarding** — si se denegó ahí (o
  el diálogo pasó desapercibido), las alertas quedaban bloqueadas para siempre sin que
  nada lo volviera a pedir. Ahora también se pide EN CONTEXTO al pulsar Iniciar si falta
  (Android 13+); la escucha arranca igual — el flash avisa aunque se deniegue.
- **Observabilidad en vivo (patrón de #38/#39, tercera vez que paga):** la pantalla
  muestra "Oyendo: <top-3 scores>" en tiempo real mientras escucha y "Última alerta:".
  Si la próxima prueba siguiera muda, los scores en pantalla dirán si el clasificador
  no oye (no fluyen) o si los umbrales bloquean (fluyen bajos) — y se tunean con datos
  reales del S26, no a ciegas.
- **allowBackup="false" (petición de desinstalación limpia):** los PERMISOS los resetea
  el SO al desinstalar SIEMPRE (nada que hacer ahí) — lo que hacía que una reinstalación
  no pareciera "de cero" era Auto Backup restaurando los DATOS (DataStore con tema/
  contadores/config y la sesión de Firebase Auth). Con false, desinstalar = borrado
  total real: reinstalar vuelve a pedir todo, onboarding incluido.

### 2026-08-14 (lección #41 — al cambiar una regla de dominio, barrer TODOS sus tests, no los visibles)
- CI rojo en v0.9.25: `SoundDetectionEngineTest > reset limpia rachas y cooldown` — usaba
  Doorbell (TIMBRE, ahora transitorio → dispara a la primera ventana) y su aserción de
  "racha por acumular" quedó invalidada. Al introducir `transientSound` migré los DOS
  tests que tenía a la vista y no barrí el fichero entero: el de reset estaba más abajo.
- Fix: migrado a "Alarm clock"/DESPERTADOR (sostenido) como los otros dos. Barrido
  exhaustivo hecho esta vez: de todas las aserciones isEmpty con etiquetas transitorias,
  SOLO esa dependía del debounce (las demás prueban umbral/cooldown/desactivada — inmunes).
- Regla operativa nueva: cambio de comportamiento en regla de dominio ⇒ grep de TODOS los
  tests por los INPUTS afectados (etiquetas, categorías) antes de empujar, no solo por los
  nombres de test que aparecen en el rango visible.

### 2026-08-14 (lección #42 — revert del experimento pulseOff: fidelidad del patrón > estética)
- **QA de Pablo:** en SOS/Estrobo/Baliza/Morse/Música el flash "no llega a encenderse y
  apagarse del todo". Causa: el parche EXPERIMENTAL de v0.9.17 (pulseOff = atenuar al
  nivel mínimo en vez de apagar, para que la notificación del sistema de Samsung no
  parpadeara). El riesgo de resplandor residual quedó documentado como reversible en un
  solo punto — y el QA en dispositivo lo confirmó: difumina el contraste on/off.
- **Decisión de producto:** fidelidad del patrón GANA. Revertido en el punto único
  (Camera2TorchController.pulseOff() = turnOff() real). Trade-off asumido y comunicado:
  la notificación del sistema de Samsung VOLVERÁ a parpadear al ritmo del flash — la
  genera el SO con cada apagado real y no hay API para evitarlo. Nuestra notificación
  sigue en canal MIN en Samsung (invisible en barra), así que el ruido es solo el
  indicador del sistema.
- pulseOff() se CONSERVA como gancho semántico "hueco intra-patrón" (afinable por
  dispositivo en el futuro sin tocar el engine). turnOff() marca SelfOffWindow, así que
  la detección de apagados EXTERNOS sigue sin falsos positivos por pulso — verificado.
- Tests de FlashEngine reescritos a la semántica real (2 reescritos, 1 de fallback
  eliminado por redundante). Barrido #41 aplicado: ningún otro test usa
  pulseOff/lastIntensity.

### 2026-08-14 (lección #43 — la config congelada en .first(): los interruptores no llegaban al clasificador)
- **Capturas de Pablo (la observabilidad paga por cuarta vez):** con Teléfono DESACTIVADO
  la pantalla seguía mostrando "Oyendo: Telephone 0,80" — delator inequívoco: el servicio
  tomaba UNA foto de la config (`configRepo.config.first()`) al arrancar y los toggles
  tocados DURANTE la escucha jamás llegaban al clasificador. Activar "Golpes en la
  puerta" en marcha no metía "Knock" en la allowlist → "no detecta nada", literal.
- **Fix ①:** `collectLatest` sobre la config — cada cambio reconstruye clasificador+motor
  con la foto nueva (stop del viejo primero; el rebuild de MediaPipe es ~100 ms, un
  toggle = una escritura de DataStore = una emisión, sin necesidad de debounce).
- **Fix ② (física del golpe):** un golpe dura ~0,2 s dentro de la ventana de ~1 s — su
  score llega DILUIDO. El piso de ENTRADA del clasificador (MIN_SCORE 0.3) lo descartaba
  antes de que el motor ni la pantalla lo vieran. Bajado a 0.15 (es piso de entrada, no
  de alerta: el umbral real sigue en el motor por categoría).
- **Fix ③:** alivio del 30% en el umbral efectivo de los TRANSITORIOS
  (`TRANSIENT_THRESHOLD_RELIEF = 0.7f` en el motor) — misma física; el cooldown de 4 s
  sigue conteniendo falsos positivos encadenados.
- **Barrido #41 aplicado A PRIORI esta vez:** 2 tests de umbral quedaban invalidados por
  el alivio (0.4 vs 0.5→efectivo 0.35; 0.5 vs BAJA 0.7→efectivo 0.49) — migrados a
  DESPERTADor (sostenida, sin alivio) ANTES de empujar, + 1 test nuevo que fija el
  alivio (transitoria dispara a 0.4 donde la sostenida no). CI verde a la primera esperado.

### 2026-08-14 (modo Pantalla v2 — rediseño aprobado en maqueta, 4 mejoras + bug de contraste)
- **Flujo respetado:** maqueta HTML interactiva primero → "Dale" de Pablo (con mandato
  explícito de márgenes/paddings verticales consistentes, nada superpuesto) → Kotlin.
- **① BUG REAL de contraste, un carácter:** `luminance(argb) > 0.5f → Color.White` estaba
  INVERTIDO — fondo claro elegía texto BLANCO (invisible sobre blanco, captura de Pablo).
  Ahora: luminancia EFECTIVA (color × brillo) > 0.55 → chrome negro; si no, blanco.
- **② Bloqueo sin velo:** bloquear esconde TODO (aviso, candado, hoja); tocar muestra una
  pastilla transitoria ("mantén pulsado para desbloquear", 2,5 s); mantener pulsado en
  cualquier punto desbloquea. El velo negro 55% a pantalla completa (oscurecía justo la
  luz que el modo da) se elimina — decisión aprobada explícitamente en la maqueta.
- **③ Hoja de cero píxeles:** plegada no existe (antes dejaba franja con asa). Gesto de
  deslizar ↑ desde el borde inferior la abre (solo desbloqueada); asa con arrastre ↓ o
  toque la pliega. Zona de gesto invisible con semántica de botón (TalkBack la abre sin
  gesto). AnimatedVisibility slide+fade.
- **④ Secciones con ritmo vertical:** `SheetSection` (separador + título opcional +
  spacedBy(md) interno) — Preajustes / Color personalizado / Brillo / Modo íntimo.
  Slider de tono con la pista pintando el ESPECTRO completo (track lambda de M3 +
  Brush.horizontalGradient). `IntimateChip` extraído para ambas ramas.
- **Lección de empalme:** al usar un marcador de texto como ancla de fin de bloque y
  luego borrar el bloque original, el ancla queda COLGANTE (llaves 124/123). Verificar
  balance inmediatamente tras cada reemplazo estructural grande, no solo al final.
- 2 strings nuevas EN/ES (secciones). `screen_locked_title`/`screen_panel_expand` quedan
  definidas sin uso (inofensivo; el auditor exige usadas⊆definidas, no al revés).

### 2026-08-14 (lección #44 — el parámetro `track` de Slider M3 es ExperimentalMaterial3Api)
- CI rojo en c50c6b2: `ScreenLight.kt:490` — el `track` lambda del Slider (pista arcoíris)
  requiere `@OptIn(ExperimentalMaterial3Api::class)`; el fichero solo tenía el OptIn de
  Foundation. Con `abortOnError`, un experimental sin opt-in es error, no warning.
- Regla: al usar cualquier parámetro/overload nuevo de M3 (track, thumb, etc.), añadir el
  OptIn de Material3 en la misma pasada — comprobar SIEMPRE si la firma es experimental.

### 2026-08-14 (login + drawer completo — rediseño en 2 pantallas, flujo maqueta→Dale→Kotlin)
- **Login (AuthScreen):** icono de marca y subtítulo eliminados; bloque de Google subido
  ARRIBA (antes de la tarjeta de email) con el estilo oficial de la industria — icono
  `ic_google_g.xml` vectorial de 4 colores (tint=Unspecified) + `ButtonDefaults
  .outlinedButtonColors(containerColor=surface)`. Todo cabe sin scroll en un móvil normal.
- **Drawer, reordenación completa** (find-skills → kostja94/marketing-skills@copywriting
  instalado y aplicado a los textos de Pro/Herramientas):
  - **Cuenta:** pill "Correo verificado" → check compacto junto al email (texto completo
    conservado en semántica para TalkBack); "Mis datos" nuevo desplegable DENTRO de
    Cuenta (absorbe el antiguo "Perfil de facturación" + "Borrar cuenta", que se muda de
    primer nivel a dentro — acción destructiva, no debe ser lo primero que se ve).
  - **Acceso Pro:** triple redundancia → una sola línea ("2 anuncios cortos = 1 hora de
    Pro gratis."); jerarquía de CTAs CORREGIDA (estaba invertida): botón de anuncio pasa
    a `FilledTonalButton` con contador integrado ("Ver anuncio X de Y"), Suscribirse pasa
    a `Button` relleno — el relleno es la conversión Pro real, el tonal es la herramienta.
  - **Herramientas Pro** (sección NUEVA): Alerta Sonora + Letrero LED, que compartían
    patrón idéntico en 2 secciones separadas, ahora 2 `ToolRow` compactas (título +
    descripción de una línea + botón) en una sola tarjeta, insertada justo bajo Acceso
    Pro — es lo que el Pro desbloquea, colocado donde refuerza el porqué de comprar.
  - **Apariencia** (fusión): Tema + Acento + Estilo, antes 2 secciones, ahora 1 con
    labels internos (`labelMedium`) por control.
  - **Accesibilidad:** plegable, PLEGADA por defecto (antes 4 toggles siempre visibles).
  - **General** (fusión): Idioma + Acerca de (versión/novedades/valorar) en una tarjeta.
- Textos acortados con el skill (beneficio directo, sin relleno): explicativos de
  Alerta/LED y las 4 descripciones de Accesibilidad, todas a una línea.
- 8 strings nuevas EN/ES; strings ahora huérfanas (definidas sin uso, inofensivo):
  billing_section/billing_profile_row/-support/-hint, sound_alert_open, led_open,
  sound_alert_section, led_section, language_section, about_section. pro_explainer y
  pro_progress_start/-one_left NO se tocaron (esta última sigue viva en diálogos de
  BeamHub/Settings).
- Lección operativa: tras un corte de turno con llamadas duplicadas/abortadas, verificar
  el estado REAL del working tree (git status + grep de marcadores) antes de repetir
  nada — todo lo previsto ya se había aplicado; solo faltó UN import (HorizontalDivider,
  usado 3 veces en el código nuevo) que el auditor no detecta por sí solo salvo al
  compilar — revisar imports de cada símbolo nuevo usado, no solo los ya recurrentes.

### 2026-08-14 (Privacidad y Términos: WebView interno, ya no sacan de la app)
- **Petición de Pablo:** Política de privacidad y Términos abrían con `Intent.ACTION_VIEW` al
  navegador del sistema — sacaban al usuario de LumiAI. Licencias OSS (popup) se queda tal cual,
  no se toca. Patrón pedido: el de la mayoría de apps — WebView interno, un único botón de volver,
  nunca sale de la app.
- **`LegalWebScreen.kt` (nueva):** `AndroidView` envolviendo `WebView` con `Scaffold` + barra
  superior (título + flecha de volver). El back (gesto, botón físico, o la flecha de la barra) va
  primero al historial DEL PROPIO WebView (`canGoBack()`/`goBack()`) y solo cierra la pantalla
  cuando ya no hay más que deshacer — el patrón estándar.
- **Navegación restringida al mismo dominio:** `shouldOverrideUrlLoading` bloquea cualquier carga
  a un host distinto del documento — si el HTML legal enlazara algún día a otro sitio, esa carga
  se ignora en vez de seguirse. "Nunca sale de la app" cubre también los enlaces internos del
  propio documento, no solo la ausencia de barra de navegación.
- **URLs centralizadas:** `LegalUrls` en `LumiAiNavHost.kt` — un único sitio en todo el código
  donde viven `privacy-policy.html` y `terms.html`, en vez de repetidas inline como antes.
- Rutas nuevas `LEGAL_PRIVACY`/`LEGAL_TERMS` en el NavHost; `SettingsScreen` gana
  `onOpenPrivacyPolicy`/`onOpenTerms`, sustituyendo los `Intent.ACTION_VIEW` de esas dos filas
  concretas. Los otros 4 usos de `Intent.ACTION_VIEW` en el fichero (valorar en Play, gestionar
  suscripción) quedan intactos — esos SÍ deben salir de la app.
- Lección de proceso: primera escritura del fichero se me fue de las manos con funciones de
  relleno sin sentido (`fillMaxWidthCompat` vacía, un hook de pausa no-op) — reescrito limpio
  antes de conectarlo a nada, en vez de dejarlo así y "ya lo arreglo luego".

### 2026-08-16 (lección #46 — no asumir material-icons-extended: el proyecto usa drawables propios)
- CI rojo en v0.9.32: `LegalWebScreen.kt` usaba `Icons.AutoMirrored.Filled.ArrowBack`, pero
  **`material-icons-extended` no es dependencia de este proyecto** — todas las pantallas usan
  drawables vectoriales propios (`ic_back`, `ic_chevron_down`...) con `painterResource`.
- Fix: mismo patrón que `SoundAlertScreen` — `painterResource(R.drawable.ic_back)` +
  `contentDescription = stringResource(R.string.back_cd)`, que además da la etiqueta de
  accesibilidad que la versión con `Icons` tenía a `null`.
- Regla operativa: al crear una pantalla nueva, copiar el patrón de iconografía de una pantalla
  hermana ANTES de escribir imports de memoria. Barrido posterior: cero usos de `Icons.` en todo
  el proyecto, este era el único.

### 2026-08-16 (Q5 — Firebase App Check con Play Integrity, implementación robusta)
- **Qué protege:** el `google-services.json` viaja dentro del APK y es trivial de extraer. Sin
  App Check, cualquiera con ese fichero puede hablar con Auth y Firestore haciéndose pasar por
  LumiAI. App Check acredita que la petición viene de una copia legítima instalada desde Play.
- **API verificada contra la documentación oficial de Firebase (actualizada 13-ago-2026)**, no de
  memoria: `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)`. find-skills
  ejecutado: el único skill de App Check del ecosistema es de Flutter, inservible para Kotlin
  nativo — implementación propia.
- **Decisión de robustez ①: separación por SOURCE SET, no por `if (BuildConfig.DEBUG)`.**
  `firebase-appcheck-debug` se declara como `debugImplementation`, y cada variante tiene su
  propio `AppCheckInstaller` (`src/debug/...` y `src/release/...`) con firma idéntica. Resultado:
  la clase del proveedor permisivo **ni siquiera está en el APK de release** — no hay ruta de
  código, ni siquiera por reflexión, que pueda activarlo en producción. Un `if` de BuildConfig
  habría dejado la clase dentro del binario.
- **Decisión de robustez ②: no puede tumbar la app JAMÁS.** Play Integrity depende de Google Play
  Services: en dispositivos sin ellos, ROMs alternativas o Play Services corrupto, la
  inicialización puede lanzar. Una linterna que no abre por una comprobación de integridad sería
  mucho peor que el ataque del que protege. Todo va en `runCatching`; si falla, App Check queda
  inactivo y la app sigue funcionando. El fallo se registra en Crashlytics como no-fatal para
  poder detectar si ocurre de forma masiva en algún modelo concreto.
- **Orden de arranque (no es cosmético):** `AppCheckInstaller.install()` es lo PRIMERO tras
  `super.onCreate()`, antes de `purgeGodOverrideOnRelease`/`syncUserRegistryOnChange`, que hablan
  con Firestore y Auth. Instalarlo después dejaría esas primeras peticiones sin token.
- **Parámetro `context` eliminado:** la API no lo necesita (Firebase se auto-inicializa por
  ContentProvider antes de `Application.onCreate`). Dejarlo habría sido ruido que además el lint
  gate de Q4 podría marcar.
- **PASOS MANUALES PENDIENTES DE PABLO** (sin ellos App Check no protege nada):
  1. Firebase Console → App Check → registrar LumiAI con **Play Integrity** (pide el SHA-256 de
     release, el mismo del keystore).
  2. Ejecutar una build debug y copiar del Logcat el token de `DebugAppCheckProvider` → darlo de
     alta en App Check → Gestionar tokens de depuración.
  3. Dejar App Check en modo **monitorización** unos días antes de activar *enforcement*: así se
     ve el porcentaje de peticiones verificadas sin romper a nadie. Activar la aplicación forzada
     solo cuando el ratio sea alto.
