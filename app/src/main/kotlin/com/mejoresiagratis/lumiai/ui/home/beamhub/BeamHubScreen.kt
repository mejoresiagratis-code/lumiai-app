package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight
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

    val hazeState = remember { HazeState() }
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

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
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeDefaults.style(backgroundColor = surface, blurRadius = 24.dp)
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.3f))
                    )
                    if (!state.capabilities.hasFlash) {
                        Text(
                            text = stringResource(R.string.no_flash_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    ModeSettingsPanel(
                        mode = state.mode,
                        settings = state.settings,
                        caps = state.capabilities,
                        onChange = viewModel::updateSettings
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LumiSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModeRail(
                    selected = state.mode,
                    onSelect = viewModel::selectMode,
                    onLocked = { onOpenAuth() },
                    caps = state.capabilities,
                    entitlements = state.entitlements,
                    modifier = Modifier.padding(top = LumiSpacing.md)
                )
                Spacer(modifier = Modifier.weight(1f))
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
                PowerOrb(isOn = state.isOn, onToggle = viewModel::toggle)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
    ) {
        available.forEach { item ->
            val locked = !entitlements.unlocks(item.mode.tier)
            ModePill(
                item = item,
                selected = item.mode == selected,
                locked = locked,
                onClick = { if (locked) onLocked(item.mode) else onSelect(item.mode) }
            )
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
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
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
                text = stringResource(item.labelRes),
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
    modifier: Modifier = Modifier
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
    val onLabel = stringResource(if (isOn) R.string.action_off else R.string.action_on)

    Box(modifier = modifier.requiredSize(252.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(252.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = glow))
        )
        Canvas(modifier = Modifier.size(224.dp)) {
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
                .size(176.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary, primary.copy(alpha = if (isOn) 0.92f else 0.55f))
                    )
                )
                .clickable(role = Role.Button, onClickLabel = onLabel, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(56.dp)) {
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
