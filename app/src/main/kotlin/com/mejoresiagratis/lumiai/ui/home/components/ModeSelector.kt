package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode

@Composable
fun ModeSelector(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    caps: DeviceCapabilities,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FlashMode.entries.filter { it.isAvailable(caps) }.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label()) }
            )
        }
    }
}

private fun FlashMode.label(): String = when (this) {
    FlashMode.CONTINUOUS -> "Continuo"
    FlashMode.SCREEN -> "Pantalla"
    FlashMode.SOS_MORSE -> "SOS"
    FlashMode.STROBE -> "Estrobo"
}
