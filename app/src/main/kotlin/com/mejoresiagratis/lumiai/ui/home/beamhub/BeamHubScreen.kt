package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import com.mejoresiagratis.lumiai.domain.entitlement.tier
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
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
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
    viewModel: FlashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.entitlements, state.mode) {
        if (!state.entitlements.unlocks(state.mode.tier)) viewModel.selectMode(FlashMode.CONTINUOUS)
    }

    val screenActive = state.isOn && state.mode == FlashMode.SCREEN
    BackHandler(enabled = screenActive) { viewModel.toggle() }
    if (screenActive) {
        ScreenLight(
            argb = state.settings.screenArgb,
            brightness = state.settings.screenBrightness,
            onColorChange = { argb -> viewModel.updateSettings { it.copy(screenArgb = argb) } },
            onBrightnessChange = { b -> viewModel.updateSettings { it.copy(screenBrightness = b) } },
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
                            primary.copy(alpha = if (state.isOn) 0.32f else 0.12f),
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
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModeRail(
                    selected = state.mode,
                    onSelect = viewModel::selectMode,
                    onLocked = { onOpenAuth() },
                    caps = state.capabilities,
                    entitlements = state.entitlements,
                    modifier = Modifier.padding(top = LumiSpacing.md, bottom = LumiSpacing.md)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.mode.isAvailable(state.capabilities)) {
                    MODE_CATALOG.firstOrNull { it.mode == state.mode }?.let { ui ->
                        Text(
                            text = stringResource(ui.labelRes).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(bottom = LumiSpacing.md)
                        )
                    }
                    PowerOrb(
                        isOn = state.isOn,
                        onToggle = viewModel::toggle,
                        orbDiameter = orbSize,
                        pulsePeriodMs = if (state.mode == FlashMode.BEACON) state.settings.beaconIntervalMs else null,
                        pulseFlashMs = if (state.mode == FlashMode.BEACON) state.settings.beaconFlashMs else null
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.xs),
                        modifier = Modifier.padding(top = LumiSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isOn) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                        )
                        Text(
                            text = stringResource(
                                if (state.isOn) R.string.tap_to_turn_off else R.string.tap_to_turn_on
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    else -> null
}

@Composable
private fun ModeRail(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    onLocked: (FlashMode) -> Unit,
    caps: DeviceCapabilities,
    entitlements: Entitlements,
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
                val locked = !entitlements.unlocks(item.mode.tier)
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
private fun ModePill(
    item: ModeUi,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val isSel = selected
    val lockedLabel = stringResource(R.string.mode_locked_cd)
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(88.dp)
            .shadow(elevation = if (selected) 10.dp else 3.dp, shape = shape, clip = false)
            .clip(shape)
            .background(container)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
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
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = stringResource(item.shortLabelRes),
                style = MaterialTheme.typography.labelSmall,
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
private fun PowerOrb(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    orbDiameter: Dp = 252.dp,
    pulsePeriodMs: Long? = null,
    pulseFlashMs: Long? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val glow by animateFloatAsState(targetValue = if (isOn) 0.5f else 0f, label = "orbGlow")
    val scale by animateFloatAsState(targetValue = if (isOn) 1f else 0.94f, label = "orbScale")

    val transition = rememberInfiniteTransition(label = "orbBeam")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2600, easing = LinearEasing)),
        label = "orbSweep"
    )

    // Pulso sincronizado con el destello de Baliza: brillo durante el flash, atenuado en la pausa.
    val pulsing = pulsePeriodMs != null && isOn
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

    val onLabel = stringResource(if (isOn) R.string.action_off else R.string.action_on)
    val torchLabel = stringResource(R.string.a11y_torch)
    val orbStateLabel = stringResource(if (isOn) R.string.a11y_state_on else R.string.a11y_state_off)

    Box(modifier = modifier.requiredSize(orbDiameter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(orbDiameter)
                .clip(CircleShape)
                .background(primary.copy(alpha = haloAlpha))
        )
        Canvas(modifier = Modifier.size(orbDiameter * (224f / 252f))) {
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
                val lit = isOn && (((angle - sweep) % 360f + 360f) % 360f) < 60f
                drawLine(
                    color = if (lit) primary else primary.copy(alpha = if (isOn) 0.32f else 0.18f),
                    start = Offset(center.x + cosA * rInner, center.y + sinA * rInner),
                    end = Offset(center.x + cosA * rOuter, center.y + sinA * rOuter),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        Box(
            modifier = Modifier
                .size(orbDiameter * (176f / 252f))
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary, primary.copy(alpha = if (isOn) 0.92f else 0.55f))
                    )
                )
                .clickable(role = Role.Button, onClickLabel = onLabel, onClick = onToggle)
                .semantics(mergeDescendants = true) {
                    contentDescription = torchLabel
                    stateDescription = orbStateLabel
                    liveRegion = LiveRegionMode.Polite
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(orbDiameter * (56f / 252f))) {
                val sw = 7.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = (size.minDimension / 2f) - sw
                drawArc(
                    color = onPrimary,
                    startAngle = -60f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
                drawLine(
                    color = onPrimary,
                    start = Offset(center.x, center.y - r - sw * 0.4f),
                    end = Offset(center.x, center.y - r * 0.15f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
