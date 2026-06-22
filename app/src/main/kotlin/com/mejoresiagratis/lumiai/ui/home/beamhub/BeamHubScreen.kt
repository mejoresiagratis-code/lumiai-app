package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.entitlement.tier
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.home.FlashViewModel
import com.mejoresiagratis.lumiai.ui.home.components.ModeGrid
import com.mejoresiagratis.lumiai.ui.home.components.ModeSettingsPanel
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

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
        // Fuente del blur: fondo por modo a pantalla completa (lo que la hoja difumina).
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
                    title = { Text(stringResource(R.string.app_name)) },
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
                // Hoja de cristal: difumina el fondo por modo que queda detrás.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
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
                ModeGrid(
                    selected = state.mode,
                    onSelect = viewModel::selectMode,
                    onLocked = { onOpenAuth() },
                    caps = state.capabilities,
                    entitlements = state.entitlements,
                    modifier = Modifier.padding(top = LumiSpacing.md)
                )
                Spacer(modifier = Modifier.weight(1f))
                PowerOrb(isOn = state.isOn, onToggle = viewModel::toggle)
                Spacer(modifier = Modifier.weight(1f))
            }
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
    val glow by animateFloatAsState(targetValue = if (isOn) 0.45f else 0f, label = "orbGlow")
    val scale by animateFloatAsState(targetValue = if (isOn) 1f else 0.94f, label = "orbScale")

    Box(modifier = modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = glow))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary, primary.copy(alpha = if (isOn) 0.92f else 0.55f))
                    )
                )
                .clickable(role = Role.Button, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(if (isOn) R.string.action_off else R.string.action_on),
                style = MaterialTheme.typography.titleLarge,
                color = onPrimary
            )
        }
    }
}
