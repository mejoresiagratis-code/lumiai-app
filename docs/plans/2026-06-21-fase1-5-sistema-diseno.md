# Fase 1.5 — Sistema de diseño + tema claro/oscuro + grid de modos · Implementation Plan

> Se implementa tarea a tarea. Cada tarea deja el proyecto **compilando**. Es una capa **visual** sobre el motor ya probado: no cambia comportamiento de la linterna.

**Goal:** Dar a la app una base de diseño escalable (tokens semánticos, tema claro/oscuro real con toggle persistido) y un patrón **data-driven** para los modos (añadir un modo = añadir un dato), dejándola lista para crecer en pantallas (onboarding, paywall, cuenta) y modos sin retrabajo.

**Architecture:** Tokens M3 centralizados en `ui/theme` (color claro/oscuro, tipografía, espaciado 4/8dp, formas). `ThemeMode` (SYSTEM/LIGHT/DARK) persistido en un **DataStore compartido vía DI** (un solo archivo de preferencias para toda la app). Los modos se renderizan desde un catálogo `MODE_CATALOG` con un componente reutilizable `ModeCard` que ya contempla estado seleccionado y un hueco para "bloqueado/Pro" (anticipa Fases 3-5). Reglas `ui-ux-pro-max`: tokens semánticos (cero hex por pantalla), contraste ≥4.5:1 en ambos temas, iconos vectoriales (no emoji), targets ≥48dp, ritmo 4/8dp.

**Tech Stack:** Jetpack Compose Material 3 · DataStore · Hilt · Coroutines/Flow.

---

## File Structure

```
ui/theme/
├── Color.kt        # (reescribir) esquemas claro/oscuro completos
├── Type.kt         # (reescribir) escala tipográfica M3
├── Shape.kt        # (nuevo) formas M3
├── Spacing.kt      # (nuevo) tokens de espaciado 4/8dp
└── Theme.kt        # (reescribir) LumiAiTheme(themeMode, dynamicColor)

domain/model/ThemeMode.kt                       # (nuevo) enum + next()
domain/repository/ThemePreferencesRepository.kt # (nuevo) interfaz
data/settings/DataStoreThemePreferencesRepository.kt  # (nuevo)
data/settings/DataStoreFlashStateRepository.kt  # (modificar) inyecta DataStore compartido
di/DataStoreModule.kt                           # (nuevo) provee DataStore<Preferences>
di/AppModule.kt                                 # (modificar) +bind ThemePreferencesRepository
ui/theme/ThemeViewModel.kt                      # (nuevo)
MainActivity.kt                                 # (modificar) aplica themeMode

ui/home/components/
├── ModeUi.kt           # (nuevo) modelo + MODE_CATALOG
├── ModeCard.kt         # (nuevo)
├── ModeGrid.kt         # (nuevo) grid 2 columnas, no-lazy
└── ScreenLight.kt      # (modificar) tap para apagar
ui/home/HomeScreen.kt   # (reescribir) TopAppBar + toggle tema + grid + botón héroe

res/drawable/ic_mode_continuous.xml | ic_mode_screen.xml | ic_mode_sos.xml | ic_mode_strobe.xml | ic_theme.xml   # (nuevos)
res/values/strings.xml  # (modificar) textos de UI
app/src/test/.../domain/model/ThemeModeTest.kt   # (nuevo) TDD de next()
```

---

## Task 1: Tokens de diseño (color, tipografía, espaciado, formas)

**Files:** reescribir `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`; crear `ui/theme/Spacing.kt`, `ui/theme/Shape.kt`; crear `domain/model/ThemeMode.kt` y su test.

- [ ] **Step 1: ThemeMode + test que falla**

```kotlin
// domain/model/ThemeMode.kt
package com.mejoresiagratis.lumiai.domain.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}
```

```kotlin
// app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/model/ThemeModeTest.kt
package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `next cycles through all modes and wraps`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }
}
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemeModeTest*"`
Expected: FAIL.

- [ ] **Step 3: Tokens**

```kotlin
// ui/theme/Spacing.kt
package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.ui.unit.dp

object LumiSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

```kotlin
// ui/theme/Shape.kt
package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LumiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)
```

```kotlin
// ui/theme/Color.kt  (reescribir)
package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFFFB300)
private val AmberDark = Color(0xFFC68400)
private val AmberContainerDark = Color(0xFF4A3300)
private val AmberContainerLight = Color(0xFFFFE082)

val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1300),
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = Color(0xFFD7C4A1),
    onSecondary = Color(0xFF221A00),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF22272E),
    onSurfaceVariant = Color(0xFFC2C8D0),
    outline = Color(0xFF8B949E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6E5D3E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDFBF7),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFEDE7DC),
    onSurfaceVariant = Color(0xFF4C4639),
    outline = Color(0xFF7D7767),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)
```

```kotlin
// ui/theme/Type.kt  (reescribir)
package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

val LumiAiTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)
```

```kotlin
// ui/theme/Theme.kt  (reescribir)
package com.mejoresiagratis.lumiai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mejoresiagratis.lumiai.domain.model.ThemeMode

@Composable
fun LumiAiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumiAiTypography,
        shapes = LumiShapes,
        content = content
    )
}
```

- [ ] **Step 4: Test verde + build**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemeModeTest*"` → PASS
Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/theme app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/model/ThemeMode.kt app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/model/ThemeModeTest.kt
git commit -m "feat(theme): design tokens (color/type/spacing/shape) + ThemeMode"
```

---

## Task 2: DataStore compartido + repositorio de tema

**Files:** crear `di/DataStoreModule.kt`, `domain/repository/ThemePreferencesRepository.kt`, `data/settings/DataStoreThemePreferencesRepository.kt`; modificar `data/settings/DataStoreFlashStateRepository.kt` y `di/AppModule.kt`.

- [ ] **Step 1: Proveedor único de DataStore**

```kotlin
// di/DataStoreModule.kt
package com.mejoresiagratis.lumiai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumiai")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}
```

- [ ] **Step 2: Refactor de DataStoreFlashStateRepository para inyectar el DataStore compartido**

En `data/settings/DataStoreFlashStateRepository.kt`: **eliminar** la línea del delegate `private val Context.dataStore ...` y el import de `preferencesDataStore`/`ApplicationContext`/`Context`, y cambiar el constructor para recibir el `DataStore`:

```kotlin
// reemplazar la declaración de clase y el constructor por:
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
// (quitar: import android.content.Context, preferencesDataStore, ApplicationContext)

@Singleton
class DataStoreFlashStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : FlashStateRepository {
```

> Dentro de la clase, las referencias a `context.dataStore` pasan a `dataStore` directamente (las llamadas `dataStore.data`, `dataStore.edit { }` ya quedan correctas tras el cambio de nombre).

- [ ] **Step 3: Repositorio de tema**

```kotlin
// domain/repository/ThemePreferencesRepository.kt
package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

```kotlin
// data/settings/DataStoreThemePreferencesRepository.kt
package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemePreferencesRepository {

    private val key = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        runCatching { ThemeMode.valueOf(p[key] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[key] = mode.name }
    }
}
```

- [ ] **Step 4: Binding en AppModule**

En `di/AppModule.kt`, añadir el import y el binding:

```kotlin
import com.mejoresiagratis.lumiai.data.settings.DataStoreThemePreferencesRepository
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
```

```kotlin
    @Binds
    @Singleton
    abstract fun bindThemeRepo(impl: DataStoreThemePreferencesRepository): ThemePreferencesRepository
```

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/di app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/repository/ThemePreferencesRepository.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/data/settings
git commit -m "refactor(data): shared DataStore via DI + theme preferences repo"
```

---

## Task 3: ThemeViewModel + aplicar tema en MainActivity

**Files:** crear `ui/theme/ThemeViewModel.kt`; modificar `MainActivity.kt`.

- [ ] **Step 1: ThemeViewModel**

```kotlin
// ui/theme/ThemeViewModel.kt
package com.mejoresiagratis.lumiai.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemePreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun cycle() {
        viewModelScope.launch { repo.setThemeMode(themeMode.value.next()) }
    }
}
```

- [ ] **Step 2: MainActivity aplica el tema**

Reescribir el cuerpo de `setContent` y `LumiAiApp` en `MainActivity.kt`:

```kotlin
// imports nuevos
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.theme.ThemeViewModel
```

```kotlin
        setContent { LumiAiApp() }
    }
}

@Composable
private fun LumiAiApp(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val themeMode: ThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    LumiAiTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LumiAiNavHost()
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/theme/ThemeViewModel.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/MainActivity.kt
git commit -m "feat(theme): ThemeViewModel and apply persisted theme in MainActivity"
```

---

## Task 4: Iconos + catálogo de modos + ModeCard + ModeGrid

**Files:** crear 4 drawables de modo + `ic_theme.xml`; crear `ui/home/components/ModeUi.kt`, `ModeCard.kt`, `ModeGrid.kt`; añadir strings.

- [ ] **Step 1: Drawables vectoriales (iconos, no emoji)**

```xml
<!-- res/drawable/ic_mode_continuous.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M9,2h6v6l-1,3h-4l-1,-3z M10,12h4v6a2,2 0,0 1,-4 0z" />
</vector>
```

```xml
<!-- res/drawable/ic_mode_screen.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M5,3h14a2,2 0,0 1,2 2v14a2,2 0,0 1,-2 2H5a2,2 0,0 1,-2,-2V5a2,2 0,0 1,2,-2zM7,5v14h10V5z" />
</vector>
```

```xml
<!-- res/drawable/ic_mode_sos.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M4,10h3v4H4z M10,8h4v8h-4z M17,10h3v4h-3z" />
</vector>
```

```xml
<!-- res/drawable/ic_mode_strobe.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M13,2L4,14h6l-1,8 9,-12h-6z" />
</vector>
```

```xml
<!-- res/drawable/ic_theme.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M12,3a9,9 0,1 0,0 18,7 7,0 0,1 0,-14 9,9 0,0 0,0,-4z" />
</vector>
```

- [ ] **Step 2: Strings**

En `res/values/strings.xml` añadir dentro de `<resources>`:

```xml
    <string name="mode_continuous">Continuo</string>
    <string name="mode_screen">Pantalla</string>
    <string name="mode_sos">SOS</string>
    <string name="mode_strobe">Estrobo</string>
    <string name="action_on">Encender</string>
    <string name="action_off">Apagar</string>
    <string name="no_flash_hint">Este dispositivo no tiene flash: usa el modo Pantalla.</string>
    <string name="settings_intensity">Intensidad: %1$d%%</string>
    <string name="settings_frequency">Frecuencia: %1$s Hz</string>
    <string name="settings_speed">Velocidad (unidad): %1$d ms</string>
    <string name="theme_toggle_cd">Cambiar tema</string>
    <string name="screen_exit_cd">Tocar para apagar</string>
```

- [ ] **Step 3: Catálogo de modos**

```kotlin
// ui/home/components/ModeUi.kt
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.FlashMode

data class ModeUi(
    val mode: FlashMode,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val isPro: Boolean = false
)

val MODE_CATALOG: List<ModeUi> = listOf(
    ModeUi(FlashMode.CONTINUOUS, R.string.mode_continuous, R.drawable.ic_mode_continuous),
    ModeUi(FlashMode.SCREEN, R.string.mode_screen, R.drawable.ic_mode_screen),
    ModeUi(FlashMode.SOS_MORSE, R.string.mode_sos, R.drawable.ic_mode_sos),
    ModeUi(FlashMode.STROBE, R.string.mode_strobe, R.drawable.ic_mode_strobe)
)
```

- [ ] **Step 4: ModeCard (target ≥48dp, press feedback nativo, estado seleccionado)**

```kotlin
// ui/home/components/ModeCard.kt
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

@Composable
fun ModeCard(
    item: ModeUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 96.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(LumiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = LumiSpacing.sm)
            )
        }
    }
}
```

- [ ] **Step 5: ModeGrid (2 columnas, sin scroll anidado)**

```kotlin
// ui/home/components/ModeGrid.kt
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

@Composable
fun ModeGrid(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        MODE_CATALOG.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
            ) {
                rowItems.forEach { item ->
                    ModeCard(
                        item = item,
                        selected = item.mode == selected,
                        onClick = { onSelect(item.mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
```

- [ ] **Step 6: Build + commit**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
```bash
git add app/src/main/res/drawable/ic_mode_continuous.xml app/src/main/res/drawable/ic_mode_screen.xml app/src/main/res/drawable/ic_mode_sos.xml app/src/main/res/drawable/ic_mode_strobe.xml app/src/main/res/drawable/ic_theme.xml app/src/main/res/values/strings.xml app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeUi.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeCard.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeGrid.kt
git commit -m "feat(ui): data-driven mode catalog with ModeCard + ModeGrid"
```

---

## Task 5: HomeScreen rediseñada (TopAppBar + toggle de tema + grid + botón héroe)

**Files:** modificar `ui/home/components/ScreenLight.kt`; reescribir `ui/home/HomeScreen.kt`. `ModeSelector.kt` queda **huérfano** → eliminarlo.

- [ ] **Step 1: ScreenLight tappable (arregla el bug de Fase 1: no se podía apagar en modo Pantalla)**

```kotlin
// ui/home/components/ScreenLight.kt  (reescribir)
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mejoresiagratis.lumiai.R

@Composable
fun ScreenLight(argb: Int, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val cd = stringResource(R.string.screen_exit_cd)
    Box(
        modifier
            .fillMaxSize()
            .background(Color(argb))
            .clickable(onClick = onTap)
            .semantics { contentDescription = cd }
    )
}
```

- [ ] **Step 2: HomeScreen rediseñada**

```kotlin
// ui/home/HomeScreen.kt  (reescribir)
package com.mejoresiagratis.lumiai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.home.components.ModeGrid
import com.mejoresiagratis.lumiai.ui.home.components.ModeSettingsPanel
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import com.mejoresiagratis.lumiai.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FlashViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isOn && state.mode == FlashMode.SCREEN) {
        ScreenLight(argb = state.settings.screenArgb, onTap = viewModel::toggle)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = themeViewModel::cycle) {
                        Icon(
                            painter = painterResource(R.drawable.ic_theme),
                            contentDescription = stringResource(R.string.theme_toggle_cd)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LumiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
        ) {
            ModeGrid(
                selected = state.mode,
                onSelect = viewModel::selectMode,
                modifier = Modifier.padding(top = LumiSpacing.md)
            )
            ModeSettingsPanel(
                mode = state.mode,
                settings = state.settings,
                maxIntensity = state.maxIntensity,
                onChange = viewModel::updateSettings
            )
            if (!state.hasFlash && state.mode != FlashMode.SCREEN) {
                Text(
                    text = stringResource(R.string.no_flash_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = viewModel::toggle,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(if (state.isOn) R.string.action_off else R.string.action_on))
            }
        }
    }
}
```

> Nota: `ModeSettingsPanel` ya existe de la Fase 1; en este paso se actualizan sus textos para usar `stringResource(R.string.settings_intensity, ...)`, `settings_frequency`, `settings_speed` en lugar de literales (cambio cosmético, misma lógica).

- [ ] **Step 3: Eliminar el componente huérfano**

```bash
git rm app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeSelector.kt
```

- [ ] **Step 4: Build + commit**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/HomeScreen.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ScreenLight.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeSettingsPanel.kt
git commit -m "feat(ui): redesigned home (top bar + theme toggle + mode grid + hero button)"
```

---

## Task 6: Verificación

- [ ] **Step 1: Suite + build**

Run: `./gradlew :app:testDebugUnitTest` → PASS (incluye ThemeModeTest + los de Fase 1)
Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

- [ ] **Step 2: Push + CI verde**

```bash
git push origin main
```
Verificar run por `head_sha`: pasos `Unit tests` y `Build debug APK` en verde.

- [ ] **Step 3: Checklist visual on-device**

- [ ] El toggle del top bar cicla Sistema → Claro → Oscuro y **persiste** tras reabrir.
- [ ] Grid de 4 modos: la tarjeta seleccionada resalta (primaryContainer); el resto, surfaceVariant.
- [ ] Texto legible y con contraste suficiente en **ambos** temas.
- [ ] Modo Pantalla: tocar la pantalla blanca **apaga** (bug de Fase 1 corregido).
- [ ] Botón Encender/Apagar grande (≥48dp) con feedback de pulsación.
- [ ] Añadir un 5º modo de prueba al `MODE_CATALOG` aparece automáticamente en el grid (validar el patrón data-driven), luego revertir.

---

## Self-Review (hecho)

- **Cobertura:** tokens (T1), tema claro/oscuro real + persistencia (T1-T3), patrón data-driven de modos (T4), Home rediseñada + bug de modo Pantalla corregido (T5). ✔
- **Sin huérfanos:** `ModeSelector.kt` se elimina explícitamente (T5/Step 3). ✔
- **Sin romper lo anterior:** el refactor del DataStore mantiene la API de `FlashStateRepository`; los fakes de test no cambian; el motor no se toca. ✔
- **Consistencia de tipos:** `ThemeMode`, `ThemePreferencesRepository`, `ThemeViewModel`, `LumiSpacing`, `LumiShapes`, `ModeUi`/`MODE_CATALOG`, `ModeCard`, `ModeGrid` se usan con la misma firma en todas las tareas. ✔
- **Riesgo:** el grid es no-lazy (4 items) — correcto a esta escala; si crecen mucho los modos, migrar a `LazyVerticalGrid` con su propio scroll. Fuentes/identidad de marca y motion quedan para el pulido de Fase 6.
