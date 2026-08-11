package com.mejoresiagratis.lumiai.ui.home.components

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.ui.theme.LocalAutoLockScreen
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.model.ScreenAnimation
import com.mejoresiagratis.lumiai.domain.model.ScreenAtmosphere
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import kotlin.math.abs
import kotlin.random.Random

/** Presets de color para el Modo Pantalla (el LED es monocromo; el color solo es posible aquí). */
val SCREEN_PRESETS: List<Int> = listOf(
    0xFFFFFFFF.toInt(), // Blanco
    0xFFFFE6B0.toInt(), // Cálido
    0xFFFF3B30.toInt(), // Rojo (noche)
    0xFF4D8BFF.toInt(), // Azul
    0xFF57D08A.toInt(), // Verde
    0xFFC9A6FF.toInt()  // Violeta
)

/** Preset nombrado: fija color + brillo de una vez para un uso típico del Modo Pantalla. */
data class ScreenPreset(val labelRes: Int, val argb: Int, val brightness: Float)

/** Atajos honestos: solo color y brillo, sin prometer nada que la pantalla no haga. */
val SCREEN_NAMED_PRESETS: List<ScreenPreset> = listOf(
    ScreenPreset(R.string.screen_preset_white, 0xFFFFFFFF.toInt(), 1f),       // Blanco máximo
    ScreenPreset(R.string.screen_preset_warm, 0xFFFFE6B0.toInt(), 0.85f),     // Cálido
    ScreenPreset(R.string.screen_preset_reading, 0xFFFFF2D6.toInt(), 0.55f),  // Lectura (cálido suave)
    ScreenPreset(R.string.screen_preset_night, 0xFFFF3B30.toInt(), 0.22f)     // Rojo nocturno (visión nocturna)
)

/** Opciones del temporizador de apagado del Modo Íntimo, en minutos (0 = infinito). */
val INTIMATE_SLEEP_OPTIONS: List<Int> = listOf(0, 15, 30, 60)

/**
 * Modo Pantalla, con el Modo Íntimo como extensión conmutable (mismo bloqueo, wakelock
 * y control de brillo de ventana ya probados; solo cambia el fondo y el rango de brillo).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenLight(
    argb: Int,
    brightness: Float,
    intimateEnabled: Boolean,
    intimateAtmosphere: ScreenAtmosphere,
    intimateAnimation: ScreenAnimation,
    intimateSleepMinutes: Int,
    onColorChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onIntimateToggle: (Boolean) -> Unit,
    onAtmosphereChange: (ScreenAtmosphere) -> Unit,
    onAnimationChange: (ScreenAnimation) -> Unit,
    onSleepMinutesChange: (Int) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.screen_exit_cd)
    val lockCd = stringResource(R.string.screen_lock_cd)
    val lockedTitle = stringResource(R.string.screen_locked_title)
    val lockedHint = stringResource(R.string.screen_locked_hint)
    val lockedCd = stringResource(R.string.screen_locked_cd)
    val window = (LocalContext.current as? Activity)?.window

    // Brillo efectivo: el Modo Íntimo capa un techo bajo para no deslumbrar a oscuras.
    val effectiveBrightness = if (intimateEnabled) {
        brightness.coerceIn(FlashSettings.MIN_INTIMATE_BRIGHTNESS, FlashSettings.MAX_INTIMATE_BRIGHTNESS)
    } else {
        brightness
    }

    // Fundido a negro del temporizador de sueño: en vez de un corte brusco, atenúa y sale.
    var fadingOut by remember { mutableStateOf(false) }
    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadingOut) 1f else 0f,
        animationSpec = tween(durationMillis = 2200),
        label = "intimateFadeOut",
        finishedListener = { if (fadingOut) onTap() }
    )
    var remainingSec by remember(intimateEnabled, intimateSleepMinutes) {
        mutableIntStateOf(if (intimateEnabled) intimateSleepMinutes * 60 else 0)
    }
    LaunchedEffect(intimateEnabled, intimateSleepMinutes) {
        if (!intimateEnabled || intimateSleepMinutes <= 0) return@LaunchedEffect
        remainingSec = intimateSleepMinutes * 60
        while (remainingSec > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSec--
        }
        fadingOut = true
    }

    // Forzar el brillo de la ventana mientras dura el modo; restaurar al salir.
    LaunchedEffect(effectiveBrightness) {
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = effectiveBrightness.coerceIn(
                FlashSettings.MIN_SCREEN_BRIGHTNESS, FlashSettings.MAX_SCREEN_BRIGHTNESS
            )
            it.attributes = lp
        }
    }
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val lp = it.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
            }
        }
    }

    val hue = remember(argb) { FloatArray(3).also { AndroidColor.colorToHSV(argb, it) }[0] }
    val onColor = if (intimateEnabled || AndroidColor.luminance(argb) > 0.5f) Color.White else Color.Black
    // Panel de ajustes ocultable: colapsado deja solo el asa superior para reabrirlo y
    // maximiza la superficie de luz; tocar fuera del panel sigue saliendo del modo.
    var panelExpanded by remember { mutableStateOf(true) }
    val autoLockScreen = LocalAutoLockScreen.current
    // El bloqueo arranca según el ajuste real de auto-bloqueo CADA vez que se entra al modo.
    // (Antes: rememberSaveable persistía un `true` de una sesión previa en el Bundle de la
    // Activity, dejando el modo bloqueado con un overlay blanco que tapaba el panel entero y
    // sin forma evidente de salir. La clave `autoLockScreen` fuerza reevaluar al reentrar.)
    var locked by remember(autoLockScreen) { mutableStateOf(autoLockScreen) }

    // --- Animaciones del Modo Íntimo (independientes del brillo de ventana) ---
    val transition = rememberInfiniteTransition(label = "intimateGlow")
    // "Respiración con latido": envolvente suave (inhalar/exhalar) con un doble pulso marcado
    // en el punto álgido, para que se note claramente sin ser agobiante. Rango 0.40→1.0 (antes
    // 0.55→1.0, apenas perceptible) y easing orgánico en vez de lineal. Ciclo de 4 s.
    val breathe by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0.40f at 0 using FastOutSlowInEasing        // valle (exhalado)
                0.85f at 900 using FastOutSlowInEasing       // primer latido sube
                0.70f at 1300 using FastOutSlowInEasing      // pequeño reflujo (sístole/diástole)
                1.00f at 2000 using FastOutSlowInEasing      // pico (inhalado pleno)
                0.75f at 3000 using FastOutSlowInEasing      // baja suave
                0.40f at 4000 using FastOutSlowInEasing      // vuelve al valle
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "intimateBreathe"
    )
    var flicker by remember { mutableStateOf(1f) }
    LaunchedEffect(intimateEnabled, intimateAnimation) {
        if (!intimateEnabled || intimateAnimation != ScreenAnimation.LLAMA) {
            flicker = 1f
            return@LaunchedEffect
        }
        while (true) {
            flicker = Random.nextInt(85, 101) / 100f
            kotlinx.coroutines.delay(Random.nextLong(50, 150))
        }
    }
    val glowAlpha = when {
        !intimateEnabled -> 1f
        intimateAnimation == ScreenAnimation.RESPIRACION -> breathe
        intimateAnimation == ScreenAnimation.LLAMA -> flicker
        else -> 1f
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                if (intimateEnabled) {
                    Brush.verticalGradient(
                        listOf(
                            Color(intimateAtmosphere.top).copy(alpha = glowAlpha),
                            Color(intimateAtmosphere.bottom).copy(alpha = glowAlpha)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color(argb), Color(argb)))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
            .semantics { contentDescription = cd }
    ) {
        Text(
            text = stringResource(R.string.screen_tap_off),
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                // El fondo debe seguir siendo full-bleed (es la fuente de luz), asi que los
                // insets se aplican al CONTENIDO, no al Box raiz. Con edge-to-edge (API 36)
                // un padding fijo no basta: el texto quedaria bajo la barra de estado.
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = LumiSpacing.lg)
        )

        if (!locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // En apaisado la barra de navegacion se coloca en el lateral DERECHO,
                    // justo donde vive este boton: sin systemBarsPadding quedaria debajo y
                    // seria intocable. El recorte de camara tambien cae de lado.
                    .systemBarsPadding()
                    .displayCutoutPadding()
                    .padding(LumiSpacing.lg)
                    .clip(CircleShape)
                    .background(onColor.copy(alpha = 0.10f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { locked = true }
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = lockCd },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = onColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // El panel no tenia NI tope de altura NI scroll: crecia hasta caber su contenido, y en
        // apaisado (~393dp de alto) eso significaba tapar la pantalla entera, incluido el
        // "toca fuera para apagar". Se acota al 72% del alto con scroll interno, y se limita
        // el ancho en superficies anchas para no estirar los controles.
        val panelMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
        Surface(
            color = Color(0xF00B0E13),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = panelMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    // Absorbe toques para no apagar al ajustar.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md)
                    .animateContentSize()
            ) {
                // Asa: toca para plegar/desplegar el panel de ajustes.
                val panelToggleLabel = stringResource(R.string.a11y_panel_toggle)
                val panelStateLabel = stringResource(
                    if (panelExpanded) R.string.a11y_panel_expanded else R.string.a11y_panel_collapsed
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { panelExpanded = !panelExpanded }
                        .semantics {
                            contentDescription = panelToggleLabel
                            role = Role.Button
                            stateDescription = panelStateLabel
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = LumiSpacing.sm)
                            .size(width = 44.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                    if (!panelExpanded) {
                        Text(
                            text = stringResource(R.string.screen_panel_expand),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = LumiSpacing.xs)
                        )
                    }
                }
                if (panelExpanded) {
                    Text(
                        text = stringResource(R.string.screen_panel_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )

                    // Chip "Modo Íntimo": alterna entre color sólido clásico y atmósferas animadas.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LumiSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                    ) {
                        val intimateLabel = stringResource(R.string.screen_intimate_toggle)
                        val intimateOnLabel = stringResource(R.string.a11y_state_on)
                        val intimateOffLabel = stringResource(R.string.a11y_state_off)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (intimateEnabled) Color.White.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (intimateEnabled) 1.5.dp else 1.dp,
                                    color = if (intimateEnabled) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { onIntimateToggle(!intimateEnabled) }
                                .semantics {
                                    role = Role.Checkbox
                                    stateDescription = if (intimateEnabled) intimateOnLabel else intimateOffLabel
                                }
                                .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.sm)
                        ) {
                            Text(
                                text = intimateLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    if (intimateEnabled) {
                        IntimateControls(
                            atmosphere = intimateAtmosphere,
                            animation = intimateAnimation,
                            sleepMinutes = intimateSleepMinutes,
                            remainingSec = remainingSec,
                            brightness = brightness,
                            onAtmosphereChange = onAtmosphereChange,
                            onAnimationChange = onAnimationChange,
                            onSleepMinutesChange = onSleepMinutesChange,
                            onBrightnessChange = onBrightnessChange
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = LumiSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                        ) {
                            SCREEN_NAMED_PRESETS.forEach { preset ->
                                val sel = preset.argb == argb && abs(brightness - preset.brightness) < 0.02f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (sel) Color.White.copy(alpha = 0.18f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            width = if (sel) 1.5.dp else 1.dp,
                                            color = if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            onColorChange(preset.argb)
                                            onBrightnessChange(preset.brightness)
                                        }
                                        .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.sm)
                                ) {
                                    Text(
                                        text = stringResource(preset.labelRes),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = LumiSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
                        ) {
                            SCREEN_PRESETS.forEach { preset ->
                                val selected = preset == argb
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(preset) }
                                )
                            }
                        }
                        val colorLabel = stringResource(R.string.screen_color)
                        Text(
                            text = colorLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = hue,
                            onValueChange = { h -> onColorChange(Color.hsv(h, 1f, 1f).toArgb()) },
                            valueRange = 0f..360f,
                            modifier = Modifier.semantics { contentDescription = colorLabel }
                        )
                        val brightnessLabel = stringResource(R.string.a11y_brightness)
                        Text(
                            text = stringResource(R.string.screen_brightness, (brightness * 100).toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = brightness,
                            onValueChange = onBrightnessChange,
                            valueRange = FlashSettings.MIN_SCREEN_BRIGHTNESS..FlashSettings.MAX_SCREEN_BRIGHTNESS,
                            modifier = Modifier.semantics { contentDescription = brightnessLabel }
                        )
                    }
                }
            }
        }

        if (locked) {
            // Velo semitransparente oscuro (no opaco al color de pantalla) para que el candado y
            // el texto "mantén pulsado para desbloquear" SIEMPRE se lean, sea cual sea el color.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = { locked = false }
                    )
                    .semantics { contentDescription = lockedCd },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = lockedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = lockedHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Fundido a negro del temporizador: por encima de todo, incluida la pantalla bloqueada.
        if (fadeAlpha > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = fadeAlpha))
            )
        }
    }
}

/** Ajustes del Modo Íntimo: atmósfera, animación, brillo capado y temporizador de sueño. */
@Composable
private fun IntimateControls(
    atmosphere: ScreenAtmosphere,
    animation: ScreenAnimation,
    sleepMinutes: Int,
    remainingSec: Int,
    brightness: Float,
    onAtmosphereChange: (ScreenAtmosphere) -> Unit,
    onAnimationChange: (ScreenAnimation) -> Unit,
    onSleepMinutesChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = LumiSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            ScreenAtmosphere.entries.forEach { atm ->
                val sel = atm == atmosphere
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(Color(atm.top), Color(atm.bottom)))
                        )
                        .border(
                            width = if (sel) 2.5.dp else 1.dp,
                            color = if (sel) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onAtmosphereChange(atm) }
                        .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.sm)
                ) {
                    Text(
                        text = stringResource(atm.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.screen_intimate_animation),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(top = LumiSpacing.md)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LumiSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            ScreenAnimation.entries.forEach { anim ->
                val sel = anim == animation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (sel) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            width = if (sel) 1.5.dp else 1.dp,
                            color = if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onAnimationChange(anim) }
                        .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.sm)
                ) {
                    Text(
                        text = stringResource(anim.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        val brightnessLabel = stringResource(R.string.a11y_brightness)
        Text(
            text = stringResource(R.string.screen_brightness, (brightness * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(top = LumiSpacing.md)
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = FlashSettings.MIN_INTIMATE_BRIGHTNESS..FlashSettings.MAX_INTIMATE_BRIGHTNESS,
            modifier = Modifier.semantics { contentDescription = brightnessLabel }
        )

        Text(
            text = stringResource(R.string.screen_intimate_sleep_timer),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(top = LumiSpacing.md)
        )
        if (sleepMinutes > 0) {
            val mm = remainingSec / 60
            val ss = remainingSec % 60
            Text(
                text = stringResource(R.string.screen_intimate_sleep_remaining, mm, ss),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LumiSpacing.xs, bottom = LumiSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            INTIMATE_SLEEP_OPTIONS.forEach { minutes ->
                val sel = minutes == sleepMinutes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (sel) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            width = if (sel) 1.5.dp else 1.dp,
                            color = if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSleepMinutesChange(minutes) }
                        .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.sm)
                ) {
                    Text(
                        text = if (minutes == 0) {
                            stringResource(R.string.screen_intimate_sleep_infinite)
                        } else {
                            stringResource(R.string.screen_intimate_sleep_minutes, minutes)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun ScreenAtmosphere.labelRes(): Int = when (this) {
    ScreenAtmosphere.FUEGO -> R.string.screen_atmosphere_fire
    ScreenAtmosphere.ATARDECER -> R.string.screen_atmosphere_sunset
    ScreenAtmosphere.VELA -> R.string.screen_atmosphere_candle
    ScreenAtmosphere.NEBULOSA -> R.string.screen_atmosphere_nebula
}

private fun ScreenAnimation.labelRes(): Int = when (this) {
    ScreenAnimation.ESTATICO -> R.string.screen_animation_static
    ScreenAnimation.RESPIRACION -> R.string.screen_animation_breathing
    ScreenAnimation.LLAMA -> R.string.screen_animation_flicker
}

/**
 * Baliza en pantalla para dispositivos sin LED: parpadea el display a blanco al ritmo del
 * intervalo, con el brillo de ventana al máximo mientras dura. Toca para apagar.
 */
@Composable
fun ScreenBeacon(
    intervalMs: Long,
    flashMs: Long,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.screen_exit_cd)
    val window = (LocalContext.current as? Activity)?.window

    LaunchedEffect(Unit) {
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = FlashSettings.MAX_SCREEN_BRIGHTNESS
            it.attributes = lp
        }
    }
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val lp = it.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
            }
        }
    }

    val period = intervalMs.toInt().coerceAtLeast(200)
    val flash = flashMs.toInt().coerceIn(20, period / 2)
    val fade = (flash + 90).coerceAtMost(period - 10)
    val transition = rememberInfiniteTransition(label = "screenBeacon")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = period
                1f at 0
                1f at flash
                0f at fade
                0f at period
            }
        ),
        label = "screenBeaconAlpha"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
            .semantics { contentDescription = cd }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alpha))
        )
    }
}
