package com.mejoresiagratis.lumiai.ui.led

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.LedBannerConfig
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Modo Letrero LED: marquesina de texto con estética de panel de puntos, admite emojis.
 *
 * Arquitectura de renderizado (robusta y barata):
 * 1. El texto se RASTERIZA UNA VEZ por configuración a un bitmap-rejilla de [LED_ROWS]
 *    filas: se dibuja con Paint a 4x y se reescala con filtro (emojis nítidos incluidos),
 *    y sus píxeles se leen a un IntArray. Nada de medir texto por frame.
 * 2. El scroll es un acumulador ([offsetCols]) alimentado por [withFrameNanos] y LEÍDO
 *    DENTRO del bloque de draw del Canvas (deferred read): a 60 fps solo se invalida el
 *    dibujado, sin recomposición (patrón de la skill compose-animations).
 * 3. Cada frame dibuja como mucho filas x columnas visibles círculos: acotado y estable.
 *
 * El gate (Pro o 2 anuncios) vive FUERA, en la entrada de Ajustes: aquí se llega ya
 * desbloqueado, igual que en Alerta Sonora.
 */
private const val LED_ROWS = 26
private const val GAP_COLS = 10          // separación entre repeticiones del texto
private const val RASTER_SCALE = 4       // se rasteriza a 4x y se reduce (nitidez emoji)
private const val DOT_FILL = 0.82f       // diámetro del punto respecto a la celda
private const val ALPHA_ON = 64          // umbral de alpha para considerar un punto encendido

/** Rejilla rasterizada: [cols] x [LED_ROWS] píxeles ARGB del texto. */
private class LedGrid(val cols: Int, val pixels: IntArray)

/** Rasteriza [text] con [argb] a una rejilla de LED_ROWS de alto. Emojis conservan su color. */
private fun rasterize(text: String, argb: Int): LedGrid {
    val shown = text.ifBlank { " " }
    val hiRows = LED_ROWS * RASTER_SCALE
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = argb
        textSize = hiRows * 0.78f
    }
    val fm = paint.fontMetrics
    val widthHi = paint.measureText(shown).toInt().coerceAtLeast(1)
    val hi = Bitmap.createBitmap(widthHi, hiRows, Bitmap.Config.ARGB_8888)
    AndroidCanvas(hi).drawText(shown, 0f, hiRows / 2f - (fm.ascent + fm.descent) / 2f, paint)
    val cols = (widthHi / RASTER_SCALE).coerceAtLeast(1)
    val grid = Bitmap.createScaledBitmap(hi, cols, LED_ROWS, true)
    hi.recycle()
    val pixels = IntArray(cols * LED_ROWS)
    grid.getPixels(pixels, 0, cols, 0, 0, cols, LED_ROWS)
    grid.recycle()
    return LedGrid(cols, pixels)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedBannerScreen(
    onBack: () -> Unit,
    viewModel: LedBannerViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    var running by remember { mutableStateOf(false) }

    if (running) {
        LedBannerDisplay(config = config, onExit = { running = false })
        BackHandler { running = false }
        return
    }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.led_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = stringResource(R.string.back_cd),
                            modifier = Modifier.rotate(90f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        // En pantallas anchas (tablet, plegable, apaisado) el contenido a ancho completo
        // separa las etiquetas de sus controles y estira los campos. Tope de 600dp
        // centrado, igual que en Ajustes. En movil vertical no cambia nada.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                // OJO al orden: `fillMaxSize()` fija el ancho MINIMO al del padre, y despues
                // `widthIn` no puede bajar del minimo -> el tope se ignoraba por completo.
                // Primero el tope, luego solo el alto.
                .widthIn(max = 600.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.md)
        ) {
            // Vista previa en vivo con el mismo renderizador que el display.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                LedBannerCanvas(config = config, modifier = Modifier.fillMaxSize())
            }

            // Estado de edición LOCAL (checklist #10): nunca cablear el TextField al
            // Flow persistido; se guarda al perder el foco y al pulsar Iniciar.
            var fieldState by remember(config.text) { mutableStateOf(TextFieldValue(config.text)) }
            var hasFocus by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = fieldState,
                onValueChange = { fieldState = it },
                label = { Text(stringResource(R.string.led_input_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (hasFocus && !focus.isFocused && fieldState.text != config.text) {
                            viewModel.update { it.copy(text = fieldState.text) }
                        }
                        hasFocus = focus.isFocused
                    }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                DirectionChip(
                    selected = config.scrollLeft,
                    rotation = 90f,
                    cd = stringResource(R.string.led_dir_left_cd)
                ) { viewModel.update { it.copy(scrollLeft = true) } }
                DirectionChip(
                    selected = !config.scrollLeft,
                    rotation = -90f,
                    cd = stringResource(R.string.led_dir_right_cd)
                ) { viewModel.update { it.copy(scrollLeft = false) } }
                Box(Modifier.weight(1f))
                SpeedChip(label = "−", cd = stringResource(R.string.led_speed_down_cd)) {
                    viewModel.update { it.copy(speedLevel = it.speedLevel - 1) }
                }
                SpeedChip(label = "+", cd = stringResource(R.string.led_speed_up_cd)) {
                    viewModel.update { it.copy(speedLevel = it.speedLevel + 1) }
                }
            }

            Text(
                text = stringResource(R.string.led_speed_label, config.speedLevel, LedBannerConfig.MAX_SPEED),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                LED_PALETTE.forEach { argb ->
                    val selected = config.argb == argb
                    val cd = stringResource(R.string.led_color_cd)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.update { it.copy(argb = argb) } }
                            .semantics { contentDescription = cd }
                    )
                }
            }

            Button(
                onClick = {
                    if (fieldState.text != config.text) {
                        viewModel.update { it.copy(text = fieldState.text) }
                    }
                    running = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.led_start)) }

            Text(
                text = stringResource(R.string.led_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        }
    }
}

@Composable
private fun DirectionChip(selected: Boolean, rotation: Float, cd: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            modifier = Modifier.size(22.dp).rotate(rotation)
        )
    }
}

@Composable
private fun SpeedChip(label: String, cd: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

/** Display a pantalla completa: brillo al máximo, pantalla siempre encendida, toca para salir. */
@Composable
private fun LedBannerDisplay(config: LedBannerConfig, onExit: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val window = activity?.window
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prevBrightness = window?.attributes?.screenBrightness
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = 1f
            it.attributes = lp
        }
        // Desde v0.7.2 la app ya NO se bloquea en vertical (API 36 lo prohíbe en pantallas
        // grandes), así que aquí solo garantizamos que el letrero puede girar aunque el
        // usuario tenga el giro del sistema desactivado. Se restaura el valor previo al salir.
        val prevOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        onDispose {
            window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val lp = it.attributes
                lp.screenBrightness = prevBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
            }
            activity?.requestedOrientation =
                prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val exitCd = stringResource(R.string.led_exit_cd)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExit
            )
            .semantics { contentDescription = exitCd }
    ) {
        // El FONDO negro va a sangre (es un letrero), pero la rejilla respeta el recorte de
        // camara: sin esto, las letras que pasan por debajo del notch se ven mordidas.
        LedBannerCanvas(
            config = config,
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        )
    }
}

/** Canvas del panel LED. Reutilizado por la vista previa y el display fullscreen. */
@Composable
private fun LedBannerCanvas(config: LedBannerConfig, modifier: Modifier = Modifier) {
    // Rejilla rasterizada: solo se recalcula si cambian texto o color.
    val grid = remember(config.text, config.argb) { rasterize(config.text, config.argb) }

    // Acumulador de scroll en columnas de rejilla. withFrameNanos garantiza dt real.
    var offsetCols by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(config.speedLevel, config.scrollLeft, grid) {
        var lastNanos = 0L
        val colsPerSec = config.speedLevel * 4f
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    val total = (grid.cols + GAP_COLS).toFloat()
                    offsetCols = (offsetCols + colsPerSec * dt).mod(total)
                }
                lastNanos = now
            }
        }
    }

    val dimDot = Color(0xFF1A1D24L)
    Canvas(modifier = modifier.background(Color.Black)) {
        val cell = size.height / LED_ROWS
        if (cell <= 0f) return@Canvas
        val visibleCols = (size.width / cell).toInt() + 1
        val radius = cell * DOT_FILL / 2f
        val total = grid.cols + GAP_COLS
        // Lectura del estado DENTRO del draw: a 60fps solo se invalida esta fase.
        val off = offsetCols
        for (vx in 0 until visibleCols) {
            val srcCol = if (config.scrollLeft) {
                ((vx + off).toInt()).mod(total)
            } else {
                ((vx - off).toInt()).mod(total)
            }
            val inText = srcCol in 0 until grid.cols
            for (row in 0 until LED_ROWS) {
                val cx = vx * cell + cell / 2f
                val cy = row * cell + cell / 2f
                val pixel = if (inText) grid.pixels[row * grid.cols + srcCol] else 0
                val alpha = pixel ushr 24
                if (alpha > ALPHA_ON) {
                    drawCircle(color = Color(pixel), radius = radius, center = Offset(cx, cy))
                } else {
                    drawCircle(color = dimDot, radius = radius * 0.85f, center = Offset(cx, cy))
                }
            }
        }
    }
}

/** Paleta LED clásica: verde, rojo, azul, ámbar, cian, magenta, blanco. */
private val LED_PALETTE = listOf(
    0xFF2BE04AL.toInt(), 0xFFFF3B30L.toInt(), 0xFF3D7BFFL.toInt(), 0xFFFFB300L.toInt(),
    0xFF00E5FFL.toInt(), 0xFFFF2BD6L.toInt(), 0xFFFFFFFFL.toInt()
)
