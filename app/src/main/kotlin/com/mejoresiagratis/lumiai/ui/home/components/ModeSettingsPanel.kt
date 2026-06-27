package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.flash.ModeControl
import com.mejoresiagratis.lumiai.domain.flash.controls
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.flash.Morse
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

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
        if (mode == FlashMode.STROBE) {
            Text(
                text = stringResource(R.string.strobe_photosensitivity_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (mode == FlashMode.SOS_MORSE) {
            MorsePreview(
                symbols = Morse.toSymbols("SOS"),
                cycleMs = Morse.patternDurationMs("SOS", settings.morseUnitMs) + settings.morseUnitMs * 7,
                modifier = Modifier.padding(top = LumiSpacing.sm)
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
        if (ModeControl.BEACON_INTERVAL in controls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = { onChange { it.copy(beaconIntervalMs = 2000L, beaconFlashMs = 150L) } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.beacon_preset_locator)) }
                OutlinedButton(
                    onClick = { onChange { it.copy(beaconIntervalMs = 600L, beaconFlashMs = 200L) } },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.beacon_preset_highvis)) }
            }
            Text(stringResource(R.string.settings_beacon_interval, settings.beaconIntervalMs.toInt()))
            Slider(
                value = settings.beaconIntervalMs.toFloat(),
                onValueChange = { v -> onChange { it.copy(beaconIntervalMs = v.toLong()) } },
                valueRange = FlashSettings.MIN_BEACON_INTERVAL.toFloat()..FlashSettings.MAX_BEACON_INTERVAL.toFloat()
            )
            Text(stringResource(R.string.settings_beacon_flash, settings.beaconFlashMs.toInt()))
            Slider(
                value = settings.beaconFlashMs.toFloat(),
                onValueChange = { v -> onChange { it.copy(beaconFlashMs = v.toLong()) } },
                valueRange = FlashSettings.MIN_BEACON_FLASH.toFloat()..FlashSettings.MAX_BEACON_FLASH.toFloat()
            )
            Text(
                text = stringResource(R.string.settings_beacon_autooff),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
            ) {
                listOf(0, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = settings.beaconAutoOffMin == m,
                        onClick = { onChange { it.copy(beaconAutoOffMin = m) } },
                        label = {
                            Text(
                                if (m == 0) {
                                    stringResource(R.string.autooff_off)
                                } else {
                                    stringResource(R.string.autooff_minutes, m)
                                }
                            )
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.beacon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                MorsePreview(
                    symbols = Morse.toSymbols(settings.morseText),
                    cycleMs = Morse.patternDurationMs(settings.morseText, settings.morseUnitMs) +
                        settings.morseUnitMs * 7,
                    modifier = Modifier.padding(top = LumiSpacing.sm)
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

/**
 * Vista previa del patrón Morse en puntos (·) y rayas (–). Muestra exactamente lo que
 * parpadeará el LED (los caracteres sin Morse se omiten) y la duración aproximada de un
 * ciclo, recordando que se repite en bucle mientras el modo esté encendido.
 */
@Composable
private fun MorsePreview(
    symbols: String,
    cycleMs: Long,
    modifier: Modifier = Modifier
) {
    if (symbols.isBlank()) return
    val glyphs = symbols.replace('.', '·').replace('-', '–')
    Column(modifier.fillMaxWidth()) {
        Text(
            text = glyphs,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.morse_preview_caption, "%.1f".format(cycleMs / 1000f)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
