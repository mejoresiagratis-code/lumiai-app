package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.flash.ModeControl
import com.mejoresiagratis.lumiai.domain.flash.controls
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.flash.Morse
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

/**
 * ¿Tiene este modo controles avanzados (más allá de la intensidad básica)?
 * Sirve para decidir si mostrar el toggle "Ver más" en la hoja.
 */
fun modeHasAdvanced(mode: FlashMode, caps: DeviceCapabilities): Boolean {
    val controls = mode.controls().filter { it.isAvailable(caps) }
    return mode == FlashMode.TEXT_MORSE ||
        ModeControl.STROBE_HZ in controls ||
        ModeControl.MORSE_SPEED in controls
}

@Composable
fun ModeSettingsPanel(
    mode: FlashMode,
    settings: FlashSettings,
    caps: DeviceCapabilities,
    expanded: Boolean,
    onChange: ((FlashSettings) -> FlashSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val controls = mode.controls().filter { it.isAvailable(caps) }
    Column(modifier.fillMaxWidth()) {
        // --- BÁSICO (siempre visible) ---
        if (mode == FlashMode.SCREEN) {
            Text(
                text = stringResource(R.string.screen_settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (ModeControl.INTENSITY in controls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_intensity_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${settings.intensityLevel}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.intensityLevel.toFloat(),
                onValueChange = { v -> onChange { it.copy(intensityLevel = v.toInt()) } },
                valueRange = FlashSettings.MIN_INTENSITY.toFloat()..FlashSettings.MAX_INTENSITY.toFloat()
            )
        }

        // --- AVANZADO (plegable) ---
        if (expanded) {
            if (mode == FlashMode.TEXT_MORSE) {
                val unsupported = Morse.unsupportedChars(settings.morseText)
                OutlinedTextField(
                    value = settings.morseText,
                    onValueChange = { v -> onChange { it.copy(morseText = v.take(FlashSettings.MAX_MORSE_LEN)) } },
                    label = { Text(stringResource(R.string.settings_morse_text)) },
                    singleLine = true,
                    isError = unsupported.isNotEmpty(),
                    supportingText = if (unsupported.isNotEmpty()) {
                        {
                            Text(
                                stringResource(
                                    R.string.morse_unsupported_chars,
                                    unsupported.joinToString(" ")
                                )
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (ModeControl.STROBE_HZ in controls) {
                Text(stringResource(R.string.settings_frequency, "%.1f".format(settings.strobeHz)))
                Slider(
                    value = settings.strobeHz,
                    onValueChange = { v -> onChange { it.copy(strobeHz = v) } },
                    valueRange = FlashSettings.MIN_STROBE_HZ..FlashSettings.MAX_STROBE_HZ
                )
            }
            if (ModeControl.MORSE_SPEED in controls) {
                Text(stringResource(R.string.settings_speed, settings.morseUnitMs.toInt()))
                Slider(
                    value = settings.morseUnitMs.toFloat(),
                    onValueChange = { v -> onChange { it.copy(morseUnitMs = v.toLong()) } },
                    valueRange = FlashSettings.MIN_UNIT_MS.toFloat()..FlashSettings.MAX_UNIT_MS.toFloat()
                )
            }
        }
    }
}
