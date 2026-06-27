package com.mejoresiagratis.lumiai.ui.home.components

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/** Presets de color para el Modo Pantalla (el LED es monocromo; el color solo es posible aquí). */
val SCREEN_PRESETS: List<Int> = listOf(
    0xFFFFFFFF.toInt(), // Blanco
    0xFFFFE6B0.toInt(), // Cálido
    0xFFFF3B30.toInt(), // Rojo (noche)
    0xFF4D8BFF.toInt(), // Azul
    0xFF57D08A.toInt(), // Verde
    0xFFC9A6FF.toInt()  // Violeta
)

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

        Surface(
            color = Color(0xF00B0E13),
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
            ) {
                Text(
                    text = stringResource(R.string.screen_panel_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
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
                Text(
                    text = stringResource(R.string.screen_color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Slider(
                    value = hue,
                    onValueChange = { h -> onColorChange(Color.hsv(h, 1f, 1f).toArgb()) },
                    valueRange = 0f..360f
                )
                Text(
                    text = stringResource(R.string.screen_brightness, (brightness * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = FlashSettings.MIN_SCREEN_BRIGHTNESS..FlashSettings.MAX_SCREEN_BRIGHTNESS
                )
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
