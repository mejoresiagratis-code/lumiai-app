package com.mejoresiagratis.lumiai.ui.sound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.sound.SoundAlertService
import com.mejoresiagratis.lumiai.data.sound.labelRes
import com.mejoresiagratis.lumiai.domain.sound.AlertChannel
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundReliability
import com.mejoresiagratis.lumiai.ui.theme.LumiMotion
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Pantalla del modo Alerta Sonora: divulgacion + permiso de microfono + tarjetas DESPLEGABLES
 * por categoria (acordeon: solo una abierta; activar una categoria la despliega para configurar
 * sensibilidad y canal) + control de escucha anclado abajo. El canal flash se oculta si el
 * dispositivo no tiene flash.
 *
 * Solo capa de presentacion (re-skin sobre el mockup aprobado): dominio, codec, servicio y
 * ViewModel intactos. Todos los colores salen de MaterialTheme.colorScheme, por lo que el
 * tema claro/oscuro y los 9 acentos (azul, naranja, ambar...) aplican sin codigo extra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundAlertScreen(
    onBack: () -> Unit,
    viewModel: SoundAlertViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val hasFlash = viewModel.hasFlash
    val context = LocalContext.current

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micGranted = granted }
    // Estado REAL del servicio (no un tap local optimista): si muere por cualquier
    // razon, el boton vuelve solo a "Iniciar" en vez de quedar pillado en "Parar" (QA 13-ago).
    val listening by viewModel.listening.collectAsStateWithLifecycle()
    val stopReason by viewModel.stopReason.collectAsStateWithLifecycle()
    val lastWindow by viewModel.lastWindow.collectAsStateWithLifecycle()
    val lastDetection by viewModel.lastDetection.collectAsStateWithLifecycle()

    // Acordeon: nombre de la categoria expandida (sobrevive a rotacion).
    var expandedName by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sa_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                }
            )
        },
        bottomBar = {
            ListenBar(
                listening = listening,
                stopReason = stopReason,
                lastWindow = lastWindow,
                lastDetection = lastDetection,
                micGranted = micGranted,
                anyEnabled = config.anyEnabled,
                onStart = {
                    // Doble guarda: la barra ya exige micGranted, pero el permiso puede
                    // revocarse con la pantalla abierta (ajustes en split/ventana).
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        // Ya no se asume listening=true aqui: lo confirma el propio
                        // servicio via listeningState cuando arranca de verdad (QA 13-ago).
                        // Las alertas se entregan por notificacion: si aun no esta concedido
                        // (Android 13+), se pide EN CONTEXTO — la escucha arranca igual y
                        // el flash avisa aunque se deniegue (QA 14-ago).
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        SoundAlertService.start(context)
                    } else {
                        micGranted = false
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStop = {
                    // Parada PEDIDA por el usuario: se limpia cualquier motivo anterior para
                    // que no quede un mensaje rojo de una sesion pasada (QA 22-ago).
                    viewModel.clearStopReason()
                    SoundAlertService.stop(context)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
        ) {
            DisclosureCard()

            MicCard(
                micGranted = micGranted,
                onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            SectionHeader(stringResource(R.string.sa_section_sounds))
            Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                SoundCategory.entries.forEach { category ->
                    CategoryCard(
                        category = category,
                        enabled = config.isEnabled(category),
                        expanded = expandedName == category.name,
                        sensitivity = config.sensitivity(category),
                        channel = config.channel(category),
                        hasFlash = hasFlash,
                        onToggle = { on ->
                            viewModel.setEnabled(category, on)
                            // Activar despliega para configurar; desactivar pliega.
                            expandedName = if (on) category.name
                            else expandedName.takeIf { it != category.name }
                        },
                        onExpandToggle = {
                            expandedName = if (expandedName == category.name) null else category.name
                        },
                        onSensitivity = { viewModel.setSensitivity(category, it) },
                        onChannel = { viewModel.setChannel(category, it) }
                    )
                }
            }

            OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sa_reset))
            }
            Spacer(Modifier.height(LumiSpacing.sm))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp
    )
}

@Composable
private fun DisclosureCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(stringResource(R.string.sa_what), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.sa_privacy), style = MaterialTheme.typography.bodyMedium)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.sa_not_safety),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(LumiSpacing.sm)
                )
            }
            Text(
                stringResource(R.string.sa_battery),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MicCard(micGranted: Boolean, onRequest: () -> Unit) {
    if (micGranted) return
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.sa_mic),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sa_mic_grant))
            }
        }
    }
}

/** Barra inferior con la accion principal, siempre visible sin importar el scroll. */
@Composable
private fun ListenBar(
    listening: Boolean,
    stopReason: String?,
    lastWindow: String?,
    lastDetection: String?,
    micGranted: Boolean,
    anyEnabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.sm)
        ) {
            if (listening) {
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sa_stop))
                }
                // Observabilidad en vivo (QA 14-ago): que oye el clasificador AHORA
                // (top-3 scores) — decide en una prueba si el clasificador no oye o si
                // los umbrales bloquean. Sustituye al "Escuchando..." fijo: mas util.
                Text(
                    text = if (lastWindow != null) {
                        stringResource(R.string.sa_live_window, lastWindow)
                    } else {
                        stringResource(R.string.sa_listening)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LumiSpacing.xs),
                )
                if (lastDetection != null) {
                    Text(
                        text = stringResource(R.string.sa_last_detection, lastDetection),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LumiSpacing.xs),
                    )
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = micGranted && anyEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.sa_start)) }
                // Diagnostico en el movil (QA 14-ago): si el servicio murio, aqui sale
                // el motivo EXACTO (clase y mensaje de la excepcion) — se acabo adivinar.
                if (stopReason != null) {
                    Text(
                        text = stringResource(R.string.sa_start_failed, stopReason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LumiSpacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: SoundCategory,
    enabled: Boolean,
    expanded: Boolean,
    sensitivity: Sensitivity,
    channel: AlertChannel,
    hasFlash: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit,
    onSensitivity: (Sensitivity) -> Unit,
    onChannel: (AlertChannel) -> Unit
) {
    val expandedLabel = stringResource(R.string.sa_card_expanded_cd)
    val collapsedLabel = stringResource(R.string.sa_card_collapsed_cd)
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onExpandToggle)
                    .semantics {
                        stateDescription = if (expanded) expandedLabel else collapsedLabel
                    }
                    .padding(LumiSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(iconRes = category.iconRes(), active = enabled)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(category.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ReliabilityRow(category.reliability)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
                Chevron(expanded = expanded)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = LumiMotion.emphasized()) + fadeIn(),
                exit = shrinkVertically(animationSpec = LumiMotion.standard()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = LumiSpacing.md, end = LumiSpacing.md, bottom = LumiSpacing.md
                    ),
                    verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LabeledSegmented(
                        label = stringResource(R.string.sa_sensitivity),
                        options = Sensitivity.entries,
                        selected = sensitivity,
                        optionLabel = { stringResource(it.labelRes()) },
                        onSelect = onSensitivity
                    )
                    ChannelSelector(
                        hasFlash = hasFlash,
                        channel = channel,
                        onChannel = onChannel
                    )
                }
            }
        }
    }
}

/**
 * Contenedor del icono de la categoria: squircle en reposo que muta a circulo al activarse
 * (shape-morph con muelle, mismo lenguaje que el orbe de F1). El color usa primaryContainer,
 * asi que sigue al acento elegido y al tema claro/oscuro sin nada extra.
 */
@Composable
private fun CategoryIcon(@DrawableRes iconRes: Int, active: Boolean) {
    val corner by animateDpAsState(
        targetValue = if (active) 24.dp else 16.dp,
        animationSpec = LumiMotion.emphasized(),
        label = "iconCorner"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(corner))
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Punto de color + texto de fiabilidad (alta -> primary, media -> onSurfaceVariant). */
@Composable
private fun ReliabilityRow(reliability: SoundReliability) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (reliability == SoundReliability.ALTA) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
        )
        Text(
            stringResource(reliability.labelRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = LumiMotion.emphasized(),
        label = "chevron"
    )
    Icon(
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = if (expanded) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(20.dp)
            .rotate(rotation)
    )
}

@Composable
private fun ChannelSelector(
    hasFlash: Boolean,
    channel: AlertChannel,
    onChannel: (AlertChannel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Text(
            stringResource(R.string.sa_how),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasFlash) {
            SegmentedRow(
                options = AlertChannel.entries,
                selected = channel,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onChannel
            )
        } else {
            Text(
                stringResource(R.string.sa_no_flash),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> LabeledSegmented(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedRow(options = options, selected = selected, optionLabel = optionLabel, onSelect = onSelect)
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}
            ) {
                Text(optionLabel(option), maxLines = 1)
            }
        }
    }
}

@DrawableRes
private fun SoundCategory.iconRes(): Int = when (this) {
    SoundCategory.TIMBRE -> R.drawable.ic_sound_doorbell
    SoundCategory.GOLPES_PUERTA -> R.drawable.ic_sound_knock
    SoundCategory.TELEFONO -> R.drawable.ic_sound_phone
    SoundCategory.PERRO -> R.drawable.ic_sound_dog
    SoundCategory.BEBE -> R.drawable.ic_sound_baby
    SoundCategory.DESPERTADOR -> R.drawable.ic_sound_alarm
    SoundCategory.SIRENA -> R.drawable.ic_sound_siren
    SoundCategory.ALARMA_HUMO -> R.drawable.ic_sound_smoke
}

@StringRes
private fun SoundReliability.labelRes(): Int = when (this) {
    SoundReliability.ALTA -> R.string.sa_reliab_high
    SoundReliability.MEDIA -> R.string.sa_reliab_med
}

@StringRes
private fun Sensitivity.labelRes(): Int = when (this) {
    Sensitivity.BAJA -> R.string.sa_sens_low
    Sensitivity.MEDIA -> R.string.sa_sens_med
    Sensitivity.ALTA -> R.string.sa_sens_high
}

@StringRes
private fun AlertChannel.labelRes(): Int = when (this) {
    AlertChannel.FLASH -> R.string.sa_chan_flash
    AlertChannel.PANTALLA -> R.string.sa_chan_screen
    AlertChannel.AMBAS -> R.string.sa_chan_both
}
