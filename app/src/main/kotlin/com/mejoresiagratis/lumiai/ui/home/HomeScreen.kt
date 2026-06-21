package com.mejoresiagratis.lumiai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
                .padding(horizontal = LumiSpacing.lg)
                .padding(bottom = LumiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
        ) {
            ModeGrid(
                selected = state.mode,
                onSelect = viewModel::selectMode,
                caps = state.capabilities,
                modifier = Modifier.padding(top = LumiSpacing.md)
            )
            ModeSettingsPanel(
                mode = state.mode,
                settings = state.settings,
                caps = state.capabilities,
                onChange = viewModel::updateSettings
            )
            if (!state.capabilities.hasFlash) {
                Text(
                    text = stringResource(R.string.no_flash_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = viewModel::toggle,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(if (state.isOn) R.string.action_off else R.string.action_on))
            }
        }
    }
}
