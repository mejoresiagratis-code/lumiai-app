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
