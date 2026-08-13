package com.mejoresiagratis.lumiai.ui.home.beamhub

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.entitlement.AccessState
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import com.mejoresiagratis.lumiai.domain.entitlement.Tier
import com.mejoresiagratis.lumiai.data.music.MusicFlashService
import com.mejoresiagratis.lumiai.domain.entitlement.tier
import com.mejoresiagratis.lumiai.domain.music.MusicSensitivity
import com.mejoresiagratis.lumiai.ui.components.LumiDialog
import com.mejoresiagratis.lumiai.ui.music.MusicViewModel
import com.mejoresiagratis.lumiai.ui.settings.RewardedUnlockViewModel
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.home.FlashViewModel
import com.mejoresiagratis.lumiai.ui.home.components.MODE_CATALOG
import com.mejoresiagratis.lumiai.ui.home.components.ModeSettingsPanel
import com.mejoresiagratis.lumiai.ui.home.components.ModeUi
import com.mejoresiagratis.lumiai.ui.home.components.ScreenBeacon
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight
import com.mejoresiagratis.lumiai.ui.home.components.modeHasAdvanced
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mejoresiagratis.lumiai.ui.theme.LocalHapticsEnabled
import com.mejoresiagratis.lumiai.ui.theme.LocalReduceMotion
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import com.mejoresiagratis.lumiai.ui.theme.LumiMotion
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeamHubScreen(
    onOpenSettings: () -> Unit,
    onOpenAuth: () -> Unit,
    viewModel: FlashViewModel = hiltViewModel(),
    rewardedUnlockViewModel: RewardedUnlockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proUi by rewardedUnlockViewModel.ui.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current

    LaunchedEffect(state.access, state.mode) {
        if (!state.access.unlocks(state.mode.tier)) viewModel.selectMode(FlashMode.CONTINUOUS)
    }

    val screenActive = state.isOn && state.mode == FlashMode.SCREEN
    BackHandler(enabled = screenActive) { viewModel.toggle() }
    if (screenActive) {
        ScreenLight(
            argb = state.settings.screenArgb,
            brightness = state.settings.screenBrightness,
            intimateEnabled = state.settings.intimateEnabled,
            intimateAtmosphere = state.settings.intimateAtmosphere,
            intimateAnimation = state.settings.intimateAnimation,
            intimateSleepMinutes = state.settings.intimateSleepMinutes,
            onColorChange = { argb -> viewModel.updateSettings { it.copy(screenArgb = argb) } },
            onBrightnessChange = { b -> viewModel.updateSettings { it.copy(screenBrightness = b) } },
            onIntimateToggle = { on -> viewModel.updateSettings { it.copy(intimateEnabled = on) } },
            onAtmosphereChange = { a -> viewModel.updateSettings { it.copy(intimateAtmosphere = a) } },
            onAnimationChange = { a -> viewModel.updateSettings { it.copy(intimateAnimation = a) } },
            onSleepMinutesChange = { m -> viewModel.updateSettings { it.copy(intimateSleepMinutes = m) } },
            onTap = viewModel::toggle
        )
        return
    }

    // Baliza en dispositivos sin LED: el destello va por la pantalla.
    val screenBeaconActive = state.isOn &&
        state.mode == FlashMode.BEACON &&
        !state.capabilities.hasFlash
    BackHandler(enabled = screenBeaconActive) { viewModel.toggle() }
    if (screenBeaconActive) {
        ScreenBeacon(
            intervalMs = state.settings.beaconIntervalMs,
            flashMs = state.settings.beaconFlashMs,
            onTap = viewModel::toggle
        )
        return
    }

    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var lockedDialogMode by remember { mutableStateOf<FlashMode?>(null) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted && state.mode == FlashMode.MUSIC && !state.isOn) {
            viewModel.toggle()
            MusicFlashService.start(context)
        }
    }

    val safeSelectMode: (FlashMode) -> Unit = { newMode ->
        val musicInvolved = state.mode == FlashMode.MUSIC || newMode == FlashMode.MUSIC
        if (state.isOn && musicInvolved) {
            viewModel.toggle()
            MusicFlashService.stop(context)
        }
        viewModel.selectMode(newMode)
    }

    lockedDialogMode?.let { mode ->
        val isGuest = !state.access.entitlements.hasAccount
        val lastAdPending = proUi.adsWatched >= proUi.adsPerGrant - 1 && !proUi.active
        val msgGranted = stringResource(R.string.pro_granted)
        val msgUnavailable = stringResource(R.string.pro_ad_unavailable)
        val msgProgressFmt = stringResource(R.string.pro_progress_more)

        // Accion compartida por las variantes AI: ver el anuncio recompensado.
        val watchAd: () -> Unit = {
            lockedDialogMode = null
            val act = activity
            if (act != null) {
                rewardedUnlockViewModel.watchAd(
                    activity = act,
                    onReward = { outcome ->
                        if (outcome.grantsUnlock) {
                            Toast.makeText(context, msgGranted, Toast.LENGTH_SHORT).show()
                            viewModel.selectMode(mode)
                        } else {
                            val remaining = (RewardProgress.ADS_PER_GRANT - outcome.newCount).coerceAtLeast(1)
                            Toast.makeText(context, msgProgressFmt.format(remaining), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUnavailable = {
                        Toast.makeText(context, msgUnavailable, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        when (mode.tier) {
            Tier.BASIC -> Unit // nunca bloqueado; rama exhaustiva por seguridad de tipos.
            Tier.PRO -> {
                // Estricto (reservado para futuros modos): sin puerta por anuncio.
                // Ningun modo actual usa este tier (Musica se movio a AI, 13-ago).
                LumiDialog(
                    onDismiss = { lockedDialogMode = null },
                    iconRes = R.drawable.ic_lock,
                    title = stringResource(R.string.music_locked_title),
                    body = stringResource(R.string.music_locked),
                    dismissLabel = stringResource(R.string.dialog_close)
                )
            }
            Tier.ADVANCED -> {
                // SOS, Estrobo, Baliza, Morse: SOLO cuenta (decision 13-ago). Se retira
                // el atajo de anuncio de este pop-up para no gastar impresiones en modos
                // ligeros; el desbloqueo temporal ganado en Musica/Alerta/LED sigue
                // aplicando igual (si estuviera activo, el modo no llegaria bloqueado).
                LumiDialog(
                    onDismiss = { lockedDialogMode = null },
                    iconRes = R.drawable.ic_lock,
                    title = stringResource(R.string.mode_locked_title),
                    body = stringResource(R.string.mode_locked_sign_in_only),
                    primaryLabel = stringResource(R.string.mode_unlock_sign_in),
                    onPrimary = { lockedDialogMode = null; onOpenAuth() },
                    dismissLabel = stringResource(R.string.dialog_close)
                )
            }
            Tier.AI -> {
                if (isGuest) {
                    // Invitado: se antepone el alta de cuenta SIN cerrar la puerta del
                    // anuncio — puede probar 1h de Pro sin registrarse si asi lo prefiere.
                    LumiDialog(
                        onDismiss = { lockedDialogMode = null },
                        iconRes = R.drawable.ic_lock,
                        title = stringResource(R.string.mode_locked_title),
                        body = stringResource(R.string.mode_locked_body),
                        primaryLabel = stringResource(R.string.mode_unlock_sign_in),
                        onPrimary = { lockedDialogMode = null; onOpenAuth() },
                        secondaryLabel = stringResource(
                            if (lastAdPending) R.string.pro_watch_ad_last else R.string.pro_watch_ad
                        ),
                        onSecondary = watchAd,
                        dismissLabel = stringResource(R.string.dialog_close)
                    )
                } else {
                    // Con cuenta: anuncio (1h) o suscripcion — la suscripcion se completa
                    // en Ajustes, donde ya vive el flujo real de Play Billing.
                    LumiDialog(
                        onDismiss = { lockedDialogMode = null },
                        iconRes = R.drawable.ic_lock,
                        title = stringResource(R.string.mode_locked_title),
                        body = stringResource(
                            if (lastAdPending) R.string.pro_progress_one_left else R.string.music_locked_body
                        ),
                        primaryLabel = stringResource(
                            if (lastAdPending) R.string.pro_watch_ad_last else R.string.pro_watch_ad
                        ),
                        onPrimary = watchAd,
                        secondaryLabel = stringResource(R.string.pro_subscribe_cta),
                        onSecondary = { lockedDialogMode = null; onOpenSettings() },
                        dismissLabel = stringResource(R.string.dialog_close)
                    )
                }
            }
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val sheetContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val sheetBorder = MaterialTheme.colorScheme.outlineVariant

    // Tamaños adaptativos: el sheet nunca pasa del 42% de la pantalla (con scroll interno) y
    // el orbe se encoge según la altura disponible, acotado, para no solaparse nunca con el sheet.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val sheetMaxHeight = (screenHeightDp * 0.42f).dp
    val orbSize = (screenHeightDp * 0.27f).dp.coerceIn(180.dp, 240.dp)

    // Aviso contextual del modo (Estrobo/Baliza): se muestra como toast descartable al
    // tocar el icono de info, y se cierra al pulsar en cualquier parte de la pantalla.
    var infoVisible by remember(state.mode) { mutableStateOf(false) }
    val infoTextRes = infoTextFor(state.mode)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = if (state.isOn) 0.30f else 0.06f),
                            background
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Text(
                            text = buildAnnotatedString {
                                append("Lumi")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("AI")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.settings_cd)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeDefaults.style(backgroundColor = sheetContainer, blurRadius = 24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = sheetBorder,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = LumiSpacing.md, vertical = LumiSpacing.md)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.45f))
                    )
                    var advancedExpanded by remember(state.mode) { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                        ) {
                            Text(
                                text = stringResource(R.string.control_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (infoTextRes != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { infoVisible = true }
                                        .minimumInteractiveComponentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_info),
                                        contentDescription = stringResource(R.string.info_cd),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (modeHasAdvanced(state.mode, state.capabilities)) {
                            Text(
                                text = stringResource(
                                    if (advancedExpanded) R.string.action_show_less else R.string.action_show_more
                                ) + if (advancedExpanded) " ▴" else " ▾",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { advancedExpanded = !advancedExpanded }
                            )
                        }
                    }
                    if (state.mode == FlashMode.MUSIC) {
                        MusicControls(modifier = Modifier.animateContentSize())
                    } else {
                        ModeSettingsPanel(
                            mode = state.mode,
                            settings = state.settings,
                            caps = state.capabilities,
                            expanded = advancedExpanded,
                            onChange = viewModel::updateSettings,
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModeRail(
                    selected = state.mode,
                    onSelect = safeSelectMode,
                    onLocked = { lockedDialogMode = it },
                    caps = state.capabilities,
                    access = state.access,
                    modifier = Modifier.padding(top = LumiSpacing.md, bottom = LumiSpacing.md)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.mode.isAvailable(state.capabilities)) {
                    PowerOrb(
                        isOn = state.isOn,
                        onToggle = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state.mode == FlashMode.MUSIC) {
                                when {
                                    state.isOn -> { viewModel.toggle(); MusicFlashService.stop(context) }
                                    micGranted -> { viewModel.toggle(); MusicFlashService.start(context) }
                                    else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                viewModel.toggle()
                            }
                        },
                        orbDiameter = orbSize,
                        pulsePeriodMs = if (state.mode == FlashMode.BEACON) state.settings.beaconIntervalMs else null,
                        pulseFlashMs = if (state.mode == FlashMode.BEACON) state.settings.beaconFlashMs else null
                    )
                    // Pildora de estado: fusiona el estado con el nombre del modo
                    // ("Toca para encender · Continuo"), en lugar del antiguo rotulo
                    // gigante que duplicaba el chip seleccionado del rail.
                    StatusPill(
                        isOn = state.isOn,
                        modeLabelRes = MODE_CATALOG.firstOrNull { it.mode == state.mode }?.labelRes,
                        modifier = Modifier.padding(top = LumiSpacing.lg)
                    )
                } else {
                    // Dispositivo sin flash y modo que lo necesita: ocultamos el orbe de
                    // LED (sería inútil) y guiamos de forma honesta al Modo Pantalla.
                    Text(
                        text = stringResource(R.string.no_flash_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(LumiSpacing.sm))
                    Text(
                        text = stringResource(R.string.no_flash_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = LumiSpacing.lg)
                    )
                    Spacer(modifier = Modifier.height(LumiSpacing.lg))
                    Button(onClick = { viewModel.selectMode(FlashMode.SCREEN) }) {
                        Text(stringResource(R.string.action_use_screen))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (infoVisible && infoTextRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { infoVisible = false }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = LumiSpacing.lg)
                ) {
                    Text(
                        text = stringResource(infoTextRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            horizontal = LumiSpacing.lg,
                            vertical = LumiSpacing.md
                        )
                    )
                }
            }
        }
    }
}

/** Aviso/ayuda contextual de cada modo, mostrado bajo demanda (icono de info → toast). */
private fun infoTextFor(mode: FlashMode): Int? = when (mode) {
    FlashMode.STROBE -> R.string.strobe_photosensitivity_warning
    FlashMode.BEACON -> R.string.beacon_hint
    FlashMode.MUSIC -> R.string.music_what
    else -> null
}

/** Ajustes de Musica en la hoja de Control: sensibilidad del detector (persistida). */
@Composable
private fun MusicControls(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val sensitivity by musicViewModel.sensitivity.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Text(
            text = stringResource(R.string.sa_sensitivity),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.music_sensitivity_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = MusicSensitivity.entries
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == sensitivity,
                    onClick = { musicViewModel.setSensitivity(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {}
                ) {
                    Text(
                        stringResource(
                            when (option) {
                                MusicSensitivity.BAJA -> R.string.sa_sens_low
                                MusicSensitivity.MEDIA -> R.string.sa_sens_med
                                MusicSensitivity.ALTA -> R.string.sa_sens_high
                            }
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Pildora de estado bajo el orbe: punto vivo + "estado · modo" sobre superficie elevada. */
@Composable
private fun StatusPill(
    isOn: Boolean,
    modeLabelRes: Int?,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        animationSpec = LumiMotion.effects(),
        label = "statusDot"
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.xs),
            modifier = Modifier.padding(horizontal = LumiSpacing.md, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            val stateText = stringResource(
                if (isOn) R.string.tap_to_turn_off else R.string.tap_to_turn_on
            )
            val modeText = modeLabelRes?.let { " · " + stringResource(it) }.orEmpty()
            Text(
                text = stateText + modeText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isOn) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeRail(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    onLocked: (FlashMode) -> Unit,
    caps: DeviceCapabilities,
    access: AccessState,
    modifier: Modifier = Modifier
) {
    val available = MODE_CATALOG.filter { it.mode.isAvailable(caps) }
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_mode).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = LumiSpacing.md, bottom = LumiSpacing.sm)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Spacer(modifier = Modifier.width(LumiSpacing.sm))
            available.forEach { item ->
                val locked = !access.unlocks(item.mode.tier)
                ModePill(
                    item = item,
                    selected = item.mode == selected,
                    locked = locked,
                    onClick = { if (locked) onLocked(item.mode) else onSelect(item.mode) }
                )
            }
            Spacer(modifier = Modifier.width(LumiSpacing.sm))
        }
    }
}

@Composable
internal fun ModePill(
    item: ModeUi,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    // Seleccion con caracter: shape-morph + escala 1.07 elevandose con muelle y sombra del acento.
    val cornerDp by animateDpAsState(
        targetValue = if (selected) 28.dp else 18.dp,
        animationSpec = LumiMotion.emphasized(),
        label = "pillCorner"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = LumiMotion.emphasized(),
        label = "pillScale"
    )
    val pillLift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = LumiMotion.emphasized(),
        label = "pillLift"
    )
    val shape = RoundedCornerShape(cornerDp)
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val isSel = selected
    val lockedLabel = stringResource(R.string.mode_locked_cd)
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(88.dp)
            .offset(y = pillLift)
            .scale(pillScale)
            .shadow(
                elevation = if (selected) 12.dp else 3.dp,
                shape = shape,
                clip = false,
                ambientColor = if (selected) container else Color.Black,
                spotColor = if (selected) container else Color.Black
            )
            .clip(shape)
            .background(container)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = shape
                    )
                }
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = isSel
                if (locked) stateDescription = lockedLabel
            }
            .padding(LumiSpacing.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = stringResource(item.shortLabelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = content,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = LumiSpacing.xs)
            )
        }
        if (locked) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.mode_locked_cd),
                tint = content,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
            )
        }
    }
}

@Composable
internal fun PowerOrb(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    orbDiameter: Dp = 252.dp,
    pulsePeriodMs: Long? = null,
    pulseFlashMs: Long? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val offContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val offContent = MaterialTheme.colorScheme.onSurfaceVariant
    val offBorder = MaterialTheme.colorScheme.outlineVariant
    val reduceMotion = LocalReduceMotion.current

    val innerSize = orbDiameter * (176f / 252f)
    val glow by animateFloatAsState(
        targetValue = if (isOn) 0.5f else 0f,
        animationSpec = LumiMotion.effects(),
        label = "orbGlow"
    )
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1f else 0.94f,
        animationSpec = LumiMotion.emphasized(),
        label = "orbScale"
    )
    // La corona SOLO existe encendida: fade + escala de muelle al aparecer.
    val coronaAlpha by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = LumiMotion.effects(),
        label = "coronaAlpha"
    )
    val coronaScale by animateFloatAsState(
        targetValue = if (isOn) 1f else 0.88f,
        animationSpec = LumiMotion.emphasized(),
        label = "coronaScale"
    )
    // Crossfade del icono: linterna en contorno (apagada) <-> linterna con haz (encendida).
    val iconOn by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = LumiMotion.emphasized(),
        label = "orbIcon"
    )

    val transition = rememberInfiniteTransition(label = "orbBeam")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2600, easing = LinearEasing)),
        label = "orbSweep"
    )
    // Rotacion lenta de toda la corona (26 s), solo encendida y sin reduce-motion.
    val coronaSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 26_000, easing = LinearEasing)),
        label = "coronaSpin"
    )
    // Respiracion sutil del orbe encendido (cede el sitio al pulso de Baliza).
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                1f at 0
                1.025f at 1600
                1f at 3200
            }
        ),
        label = "orbBreathe"
    )

    // Pulso sincronizado con el destello de Baliza: brillo durante el flash, atenuado en la pausa.
    val pulsing = pulsePeriodMs != null && isOn && !reduceMotion
    val period = (pulsePeriodMs ?: 0L).toInt().coerceAtLeast(200)
    val flash = (pulseFlashMs ?: 0L).toInt().coerceIn(20, period / 2)
    val fade = (flash + 90).coerceAtMost(period - 10)
    val pulse by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.12f,
        animationSpec = if (pulsing) {
            infiniteRepeatable(
                animation = keyframes {
                    durationMillis = period
                    0.55f at 0
                    0.55f at flash
                    0.12f at fade
                    0.12f at period
                }
            )
        } else {
            infiniteRepeatable(animation = tween(durationMillis = 1))
        },
        label = "orbPulse"
    )
    val haloAlpha = if (pulsing) pulse else glow
    val breathing = isOn && !reduceMotion && !pulsing
    val liveScale = scale * (if (breathing) breathe else 1f)

    val onLabel = stringResource(if (isOn) R.string.action_off else R.string.action_on)
    val torchLabel = stringResource(R.string.a11y_torch)
    val orbStateLabel = stringResource(if (isOn) R.string.a11y_state_on else R.string.a11y_state_off)

    Box(
        modifier = modifier
            .requiredSize(orbDiameter)
            .drawBehind {
                if (haloAlpha > 0f) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = haloAlpha), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = r
                        ),
                        radius = r
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(orbDiameter * (224f / 252f))
                .graphicsLayer {
                    alpha = coronaAlpha
                    scaleX = coronaScale
                    scaleY = coronaScale
                    rotationZ = if (isOn && !reduceMotion) coronaSpin else 0f
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val rOuter = size.minDimension / 2f - 2.dp.toPx()
            val rInner = rOuter - 12.dp.toPx()
            val tickWidth = 2.5.dp.toPx()
            val count = 60
            for (i in 0 until count) {
                val angle = i * 360f / count
                val a = (angle - 90f) * (PI.toFloat() / 180f)
                val cosA = cos(a)
                val sinA = sin(a)
                val lit = reduceMotion || (((angle - sweep) % 360f + 360f) % 360f) < 60f
                drawLine(
                    color = if (lit) primary else primary.copy(alpha = 0.32f),
                    start = Offset(center.x + cosA * rInner, center.y + sinA * rInner),
                    end = Offset(center.x + cosA * rOuter, center.y + sinA * rOuter),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        Box(
            modifier = Modifier
                .size(innerSize)
                .scale(liveScale)
                .shadow(
                    elevation = if (isOn) 18.dp else 8.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = if (isOn) primary else Color.Black,
                    spotColor = if (isOn) primary else Color.Black
                )
                .clip(CircleShape)
                .background(
                    if (isOn) {
                        Brush.radialGradient(listOf(primary, primary.copy(alpha = 0.92f)))
                    } else {
                        Brush.radialGradient(listOf(offContainer, offContainer))
                    }
                )
                .then(
                    if (isOn) Modifier
                    else Modifier.border(width = 1.5.dp, color = offBorder, shape = CircleShape)
                )
                .clickable(role = Role.Button, onClickLabel = onLabel, onClick = onToggle)
                .semantics(mergeDescendants = true) {
                    contentDescription = torchLabel
                    stateDescription = orbStateLabel
                    liveRegion = LiveRegionMode.Polite
                },
            contentAlignment = Alignment.Center
        ) {
            val iconSize = orbDiameter * (72f / 252f)
            Icon(
                painter = painterResource(R.drawable.ic_torch_off),
                contentDescription = null,
                tint = offContent,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = 1f - iconOn
                        scaleX = 1f - 0.3f * iconOn
                        scaleY = 1f - 0.3f * iconOn
                    }
            )
            Icon(
                painter = painterResource(R.drawable.ic_torch_on),
                contentDescription = null,
                tint = onPrimary,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = iconOn
                        scaleX = 0.7f + 0.3f * iconOn
                        scaleY = 0.7f + 0.3f * iconOn
                    }
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
