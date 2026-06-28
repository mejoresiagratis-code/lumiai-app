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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import kotlin.math.abs

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenLight(
    argb: Int,
    brightness: Float,
    onColorChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.screen_exit_cd)
    val lockCd = stringResource(R.string.screen_lock_cd)
    val lockedTitle = stringResource(R.string.screen_locked_title)
    val lockedHint = stringResource(R.string.screen_locked_hint)
    val lockedCd = stringResource(R.string.screen_locked_cd)
    val window = (LocalContext.current as? Activity)?.window

    // Forzar el brillo de la ventana mientras dura el modo; restaurar al salir.
    LaunchedEffect(brightness) {
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = brightness.coerceIn(
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
    val onColor = if (AndroidColor.luminance(argb) > 0.5f) Color.Black else Color.White
    // Panel de ajustes ocultable: colapsado deja solo el asa superior para reabrirlo y
    // maximiza la superficie de luz; tocar fuera del panel sigue saliendo del modo.
    var panelExpanded by remember { mutableStateOf(true) }
    val autoLockScreen = LocalAutoLockScreen.current
    var locked by rememberSaveable { mutableStateOf(autoLockScreen) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color(argb))
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
                .padding(top = LumiSpacing.xxl)
        )

        if (!locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
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

        Surface(
            color = Color(0xF00B0E13),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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

        if (locked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(argb))
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
                        tint = onColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = lockedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = onColor
                    )
                    Text(
                        text = lockedHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
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
