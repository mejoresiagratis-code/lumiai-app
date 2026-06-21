# Fase 2 — Onboarding de primer uso + permisos + arranque · Implementation Plan

> Se implementa tarea a tarea; cada una deja el proyecto compilando. No toca el motor de linterna.

**Goal:** Primer arranque cuidado: el splash (ya con marca) se mantiene hasta decidir destino; si es la primera vez se muestra un **onboarding** breve y honesto (qué hace la app, los modos, y por qué aparece la notificación) que termina pidiendo el permiso de **notificaciones** en contexto; las siguientes veces se entra directo a Home. Flag persistido en DataStore.

**Decisión de alcance — UMP (consentimiento de anuncios) se pospone a la Fase 4.** El SDK UMP existe solo para recoger consentimiento **antes del primer anuncio**, y los anuncios llegan en la Fase 4 (AdMob). Integrarlo ahora añade una dependencia de Google huérfana y un "consentimiento" sobre algo que no existe aún — incoherente con el principio de honestidad del proyecto. Se integrará junto a AdMob, que es donde tiene sentido y donde se inicializa.

**Architecture:** `OnboardingPreferencesRepository` (DataStore compartido) guarda `completed`. `StartViewModel` expone `onboardingCompleted: StateFlow<Boolean?>` (null = cargando) para (a) mantener el splash con `setKeepOnScreenCondition` y (b) elegir `startDestination` del NavHost sin parpadeo de pantalla. El permiso `POST_NOTIFICATIONS` se solicita **al terminar el onboarding** (no en `onCreate` a lo bruto). Reglas `ui-ux-pro-max`: tokens, targets ≥48dp, iconos vectoriales, textos claros.

**Tech Stack:** Compose Material 3 · DataStore · Hilt · core-splashscreen · Activity Result API.

---

## File Structure

```
domain/repository/OnboardingPreferencesRepository.kt        # (nuevo)
data/settings/DataStoreOnboardingPreferencesRepository.kt   # (nuevo)
di/AppModule.kt                                             # (modificar) +bind
ui/start/StartViewModel.kt                                  # (nuevo) decide arranque
ui/onboarding/OnboardingViewModel.kt                        # (nuevo)
ui/onboarding/OnboardingScreen.kt                           # (nuevo) stepper 3 pasos
ui/navigation/LumiAiNavHost.kt                              # (modificar) ruta ONBOARDING + startDestination
MainActivity.kt                                            # (modificar) mantener splash, elegir destino, quitar permiso a lo bruto
res/values/strings.xml                                     # (modificar) textos onboarding
app/src/test/.../OnboardingPreferencesRepositoryTest? -> no (DataStore real); se valida en build
```

---

## Task 1: Persistencia de onboarding + arranque

**Files:** crear `domain/repository/OnboardingPreferencesRepository.kt`, `data/settings/DataStoreOnboardingPreferencesRepository.kt`, `ui/start/StartViewModel.kt`; modificar `di/AppModule.kt`.

- [ ] **Step 1: Repositorio**

```kotlin
// domain/repository/OnboardingPreferencesRepository.kt
package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferencesRepository {
    val completed: Flow<Boolean>
    suspend fun setCompleted()
}
```

```kotlin
// data/settings/DataStoreOnboardingPreferencesRepository.kt
package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreOnboardingPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : OnboardingPreferencesRepository {

    private val key = booleanPreferencesKey("onboarding_completed")

    override val completed: Flow<Boolean> = dataStore.data.map { it[key] ?: false }

    override suspend fun setCompleted() {
        dataStore.edit { it[key] = true }
    }
}
```

- [ ] **Step 2: StartViewModel**

```kotlin
// ui/start/StartViewModel.kt
package com.mejoresiagratis.lumiai.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    onboarding: OnboardingPreferencesRepository
) : ViewModel() {
    // null = aún cargando; true/false = decisión tomada
    val onboardingCompleted: StateFlow<Boolean?> =
        onboarding.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
```

- [ ] **Step 3: Binding en AppModule** (añadir import + bind)

```kotlin
import com.mejoresiagratis.lumiai.data.settings.DataStoreOnboardingPreferencesRepository
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
```
```kotlin
    @Binds
    @Singleton
    abstract fun bindOnboardingRepo(impl: DataStoreOnboardingPreferencesRepository): OnboardingPreferencesRepository
```

- [ ] **Step 4: Build + commit**

`./gradlew :app:assembleDebug` → OK
```bash
git commit -m "feat(onboarding): persistence repo + StartViewModel for start destination"
```

---

## Task 2: OnboardingViewModel + pantalla (stepper 3 pasos)

**Files:** crear `ui/onboarding/OnboardingViewModel.kt`, `ui/onboarding/OnboardingScreen.kt`; añadir strings.

- [ ] **Step 1: ViewModel**

```kotlin
// ui/onboarding/OnboardingViewModel.kt
package com.mejoresiagratis.lumiai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: OnboardingPreferencesRepository
) : ViewModel() {
    fun complete() {
        viewModelScope.launch { repo.setCompleted() }
    }
}
```

- [ ] **Step 2: Strings** (en `res/values/strings.xml`, dentro de `<resources>`)

```xml
    <string name="onboarding_skip">Saltar</string>
    <string name="onboarding_next">Siguiente</string>
    <string name="onboarding_start">Empezar</string>
    <string name="ob1_title">Una linterna fiable</string>
    <string name="ob1_body">Ilumina al instante. Sin cámara ni permisos innecesarios.</string>
    <string name="ob2_title">Varios modos</string>
    <string name="ob2_body">Continuo, Pantalla, SOS y Estrobo. Ajusta intensidad y velocidad.</string>
    <string name="ob3_title">Sabrás cuándo está encendida</string>
    <string name="ob3_body">Mostramos una notificación mientras la linterna está activa para que puedas apagarla rápido.</string>
```

- [ ] **Step 3: Pantalla (stepper con APIs core, sin pager)**

```kotlin
// ui/onboarding/OnboardingScreen.kt
package com.mejoresiagratis.lumiai.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

private data class OnboardingPage(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val body: Int
)

private val PAGES = listOf(
    OnboardingPage(R.drawable.ic_mode_continuous, R.string.ob1_title, R.string.ob1_body),
    OnboardingPage(R.drawable.ic_mode_strobe, R.string.ob2_title, R.string.ob2_body),
    OnboardingPage(R.drawable.ic_settings, R.string.ob3_title, R.string.ob3_body)
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el resultado no bloquea el flujo */ }

    var step by rememberSaveable { mutableIntStateOf(0) }
    val isLast = step == PAGES.lastIndex
    val page = PAGES[step]

    fun finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.complete()
        onFinished()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LumiSpacing.lg)
                .padding(bottom = LumiSpacing.lg)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLast) {
                    TextButton(onClick = ::finish) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(page.icon),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LumiSpacing.lg)
                )
                Text(
                    text = stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LumiSpacing.md)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = LumiSpacing.lg),
                horizontalArrangement = Arrangement.Center
            ) {
                PAGES.indices.forEach { i ->
                    val active = i == step
                    Box(
                        modifier = Modifier
                            .padding(horizontal = LumiSpacing.xs)
                            .size(if (active) 10.dp else 8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        ) {}
                    }
                }
            }
            Button(
                onClick = { if (isLast) finish() else step++ },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
            }
        }
    }
}
```

- [ ] **Step 4: Build + commit**

```bash
git commit -m "feat(onboarding): first-run screen with contextual notification request"
```

---

## Task 3: NavHost + MainActivity (arranque sin parpadeo)

**Files:** modificar `ui/navigation/LumiAiNavHost.kt`, `MainActivity.kt`.

- [ ] **Step 1: NavHost con ruta ONBOARDING y startDestination**

```kotlin
// ui/navigation/LumiAiNavHost.kt
package com.mejoresiagratis.lumiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.home.HomeScreen
import com.mejoresiagratis.lumiai.ui.onboarding.OnboardingScreen
import com.mejoresiagratis.lumiai.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun LumiAiNavHost(
    startDestination: String,
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onSelectTheme = onSelectTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: MainActivity — mantener splash hasta decidir, elegir destino, quitar el permiso a lo bruto**

```kotlin
// MainActivity.kt
package com.mejoresiagratis.lumiai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.navigation.LumiAiNavHost
import com.mejoresiagratis.lumiai.ui.navigation.Routes
import com.mejoresiagratis.lumiai.ui.start.StartViewModel
import com.mejoresiagratis.lumiai.ui.theme.LumiAiTheme
import com.mejoresiagratis.lumiai.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val startViewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { startViewModel.onboardingCompleted.value == null }
        enableEdgeToEdge()
        setContent { LumiAiApp() }
    }
}

@Composable
private fun LumiAiApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startViewModel: StartViewModel = hiltViewModel()
) {
    val themeMode: ThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val completed: Boolean? by startViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    LumiAiTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val start = when (completed) {
                null -> null
                true -> Routes.HOME
                false -> Routes.ONBOARDING
            }
            if (start != null) {
                LumiAiNavHost(
                    startDestination = start,
                    themeMode = themeMode,
                    onSelectTheme = themeViewModel::setMode
                )
            }
        }
    }
}
```

> Nota: se elimina la solicitud de `POST_NOTIFICATIONS` de `onCreate`; ahora se pide en contexto al terminar el onboarding. El permiso sigue declarado en el Manifest.

- [ ] **Step 3: Build + commit**

```bash
git commit -m "feat(app): keep splash until start decided; route onboarding vs home"
```

---

## Task 4: Verificación

- [ ] **Step 1: Suite + build**

`./gradlew :app:testDebugUnitTest` → PASS (los de Fase 1/1.5 siguen verdes)
`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

- [ ] **Step 2: Push + CI verde** (por `head_sha`).

- [ ] **Step 3: Checklist on-device**

- [ ] Primera instalación → aparece el **onboarding** (3 pasos, Saltar / Siguiente / Empezar).
- [ ] "Empezar" o "Saltar" → pide el permiso de **notificaciones** (Android 13+) y entra a Home.
- [ ] Cerrar y reabrir → entra **directo a Home** (no repite onboarding).
- [ ] El splash se ve un instante y no hay parpadeo de pantalla equivocada.
- [ ] Con notificaciones concedidas, al encender la linterna se ve la notificación "Linterna activa".

---

## Self-Review (hecho)

- **Splash:** ya tenía marca (Fase 0); aquí solo se mantiene hasta resolver el destino. ✔
- **Permiso en contexto:** se quita el request crudo de `onCreate`; se pide tras explicar por qué (paso 3 del onboarding). ✔
- **Sin parpadeo:** `startDestination` se decide tras leer el flag; el splash cubre la carga. ✔
- **UMP:** pospuesto a Fase 4 (con AdMob), justificado. Pendiente de confirmación del usuario.
- **Riesgo bajo:** no toca motor ni repos de flash; reutiliza el DataStore compartido y los iconos existentes. Swipe entre páginas (pager) se puede añadir luego; ahora stepper con APIs core por robustez.
