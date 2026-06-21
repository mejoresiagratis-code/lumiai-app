package com.mejoresiagratis.lumiai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.home.components.ModeSelector
import com.mejoresiagratis.lumiai.ui.home.components.ModeSettingsPanel
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight

@Composable
fun HomeScreen(viewModel: FlashViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isOn && state.mode == FlashMode.SCREEN) {
        ScreenLight(argb = state.settings.screenArgb)
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeSelector(
                selected = state.mode,
                onSelect = viewModel::selectMode,
                caps = state.capabilities
            )
            ModeSettingsPanel(
                mode = state.mode,
                settings = state.settings,
                caps = state.capabilities,
                onChange = viewModel::updateSettings
            )
            Button(onClick = viewModel::toggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isOn) "Apagar" else "Encender")
            }
            if (!state.capabilities.hasFlash) {
                Text("Sin flash: solo el modo Pantalla esta disponible.")
            }
        }
    }
}
