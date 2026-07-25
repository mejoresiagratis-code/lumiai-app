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
