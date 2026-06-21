package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.domain.flash.ModeControl
import com.mejoresiagratis.lumiai.domain.flash.controls
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

@Composable
fun ModeSettingsPanel(
    mode: FlashMode,
    settings: FlashSettings,
    caps: DeviceCapabilities,
    onChange: ((FlashSettings) -> FlashSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        mode.controls().filter { it.isAvailable(caps) }.forEach { control ->
            when (control) {
                ModeControl.INTENSITY -> {
                    Text("Intensidad: ${settings.intensityLevel}%")
                    Slider(
                        value = settings.intensityLevel.toFloat(),
                        onValueChange = { v -> onChange { it.copy(intensityLevel = v.toInt()) } },
                        valueRange = FlashSettings.MIN_INTENSITY.toFloat()..FlashSettings.MAX_INTENSITY.toFloat()
                    )
                }
                ModeControl.STROBE_HZ -> {
                    Text("Frecuencia: ${"%.1f".format(settings.strobeHz)} Hz")
                    Slider(
                        value = settings.strobeHz,
                        onValueChange = { v -> onChange { it.copy(strobeHz = v) } },
                        valueRange = FlashSettings.MIN_STROBE_HZ..FlashSettings.MAX_STROBE_HZ
                    )
                }
                ModeControl.MORSE_SPEED -> {
                    Text("Velocidad (unidad): ${settings.morseUnitMs} ms")
                    Slider(
                        value = settings.morseUnitMs.toFloat(),
                        onValueChange = { v -> onChange { it.copy(morseUnitMs = v.toLong()) } },
                        valueRange = FlashSettings.MIN_UNIT_MS.toFloat()..FlashSettings.MAX_UNIT_MS.toFloat()
                    )
                }
            }
        }
    }
}
