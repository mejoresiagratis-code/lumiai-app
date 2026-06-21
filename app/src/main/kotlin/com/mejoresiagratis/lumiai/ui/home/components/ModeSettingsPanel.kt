package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

@Composable
fun ModeSettingsPanel(
    mode: FlashMode,
    settings: FlashSettings,
    maxIntensity: Int,
    onChange: ((FlashSettings) -> FlashSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (maxIntensity > 1 && mode != FlashMode.SCREEN) {
            Text("Intensidad: ${settings.intensityLevel}%")
            Slider(
                value = settings.intensityLevel.toFloat(),
                onValueChange = { v -> onChange { it.copy(intensityLevel = v.toInt()) } },
                valueRange = FlashSettings.MIN_INTENSITY.toFloat()..FlashSettings.MAX_INTENSITY.toFloat()
            )
        }
        when (mode) {
            FlashMode.STROBE -> {
                Text("Frecuencia: ${"%.1f".format(settings.strobeHz)} Hz")
                Slider(
                    value = settings.strobeHz,
                    onValueChange = { v -> onChange { it.copy(strobeHz = v) } },
                    valueRange = FlashSettings.MIN_STROBE_HZ..FlashSettings.MAX_STROBE_HZ
                )
            }
            FlashMode.SOS_MORSE -> {
                Text("Velocidad (unidad): ${settings.morseUnitMs} ms")
                Slider(
                    value = settings.morseUnitMs.toFloat(),
                    onValueChange = { v -> onChange { it.copy(morseUnitMs = v.toLong()) } },
                    valueRange = FlashSettings.MIN_UNIT_MS.toFloat()..FlashSettings.MAX_UNIT_MS.toFloat()
                )
            }
            else -> Unit
        }
    }
}
