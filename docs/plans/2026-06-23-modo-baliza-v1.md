# Modo Baliza (Beacon) · v1

Fecha: 23 jun 2026 · HEAD base: `578c2bf`
Modo nuevo **solo LED**, temporización pura, honesto (sin sensores ni "IA"). Señalización
y localización por destellos a intervalo regular. Tier **Básico** (siempre disponible).
Decisión de Pablo: **sin presets de auxilio** (no solapar con SOS); presets =
**Localización**, **Alta visibilidad** y **Personalizado**.

## Diseño (reutiliza el motor de reproducción on/off existente)
- `FlashMode.BEACON` (añadir al enum). `requiresFlash()=true` (extensión ya cubre: ≠SCREEN).
- `ModeControl` += `BEACON_INTERVAL, BEACON_FLASH` (timing → `isAvailable=true`).
  `BEACON.controls() = { INTENSITY, BEACON_INTERVAL, BEACON_FLASH }`. INTENSITY se
  gatea por `supportsTorchStrength` (se oculta si no hay soporte; nada de PWM falso).
- `FlashSettings` += `beaconIntervalMs` (def 1500), `beaconFlashMs` (def 120).
  `MIN_BEACON_INTERVAL=400` (tope anti-estrobo: por encima del umbral de fotosensibilidad,
  lo rápido es competencia de Estrobo), `MAX=5000`; `MIN_BEACON_FLASH=40`, `MAX=1000`.
  `coerced()`: además fuerza `flash ≤ interval-100` (siempre hay hueco apagado).
- `FlashEngine`: rama `BEACON -> settings.collectLatest { beacon(it.coerced()) }`;
  `beacon()` = bucle `on(intensity) delay(flash); off delay(interval-flash)` (igual patrón
  que `strobe`, cambios de ajuste en vivo sin reiniciar).
- `tier`: BEACON → `Tier.BASIC`. `modeAccentColor`: BEACON → **verde** (señalización).
- `MODE_CATALOG`: entrada BEACON (`R.string.mode_beacon`, `R.drawable.ic_mode_beacon`).
- Panel: bloque en el área **básica** (no plegable) con chips/botones de preset
  (Localización 2000/150 ms · Alta visibilidad 600/200 ms) + sliders **Intervalo** y
  **Destello**, y una nota honesta ("ayuda de señalización, no es dispositivo certificado").
- Persistencia: `FlashSettingsMapper` + `DataStore` Keys para interval/flash (`long`).

## Robustez
- **Fotosensibilidad:** intervalo mínimo 400 ms → siempre ≤ ~2.5 Hz, muy por debajo de
  3 destellos/seg; sigue siendo "baliza", no estrobo.
- **Batería:** destello corto + pausa larga = bajo consumo.
- **Honestidad:** solo temporización del LED; disclaimer breve en la UI.

## Commits (atómicos, cada uno compila)
1. **dominio** (atómico — al añadir el enum hay que cerrar todos los `when` exhaustivos a la
   vez): `FlashMode`, `ModeControls`, `FlashSettings`, `FlashEngine`, `Entitlements`, `Accent`.
2. **datos**: `FlashSettingsMapper` + `DataStoreFlashStateRepository` (persistir interval/flash).
3. **UI**: `ModeUi` (+ icono `ic_mode_beacon`), `ModeSettingsPanel`, `strings`.

## Diferido a 2ª tanda (extras del proposal)
- **Auto-apagado por tiempo** (15/30/60 min) — cross-cutting (VM/engine), va aparte.
- **Variante en Pantalla** para móviles sin flash (encaja con el estado sin-flash ya hecho).
- **Pulso del orbe sincronizado** con el destello.

## Verificación (sin build local)
Cubrir las 6 ramas `when (FlashMode)` exhaustivas (controls, FlashEngine, tier,
modeAccentColor — las otras usan `==`), balance de llaves, imports y refs. Pruebas en
dispositivo (Pablo): seleccionar Baliza, ver presets, ajustar interval/flash en vivo.
