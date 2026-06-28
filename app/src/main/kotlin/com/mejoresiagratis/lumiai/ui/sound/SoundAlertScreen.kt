package com.mejoresiagratis.lumiai.ui.sound

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.sound.SoundAlertService
import com.mejoresiagratis.lumiai.data.sound.labelRes
import com.mejoresiagratis.lumiai.domain.sound.AlertChannel
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundReliability
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Pantalla del modo Alerta Sonora: divulgacion + permiso de microfono + ajuste por categoria
 * (activacion, sensibilidad y canal de aviso) + control de escucha. El canal flash se oculta si
 * el dispositivo no tiene flash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundAlertScreen(
    onBack: () -> Unit,
    viewModel: SoundAlertViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val hasFlash = viewModel.hasFlash
    val context = LocalContext.current

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micGranted = granted }
    var listening by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sa_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back_cd)
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
                .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
        ) {
            DisclosureCard()

            MicCard(
                micGranted = micGranted,
                onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            SectionHeader(stringResource(R.string.sa_section_sounds))
            Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                SoundCategory.entries.forEach { category ->
                    CategoryCard(
                        category = category,
                        enabled = config.isEnabled(category),
                        sensitivity = config.sensitivity(category),
                        channel = config.channel(category),
                        hasFlash = hasFlash,
                        onToggle = { viewModel.setEnabled(category, it) },
                        onSensitivity = { viewModel.setSensitivity(category, it) },
                        onChannel = { viewModel.setChannel(category, it) }
                    )
                }
            }

            ListenCard(
                listening = listening,
                micGranted = micGranted,
                onStart = { SoundAlertService.start(context); listening = true },
                onStop = { SoundAlertService.stop(context); listening = false }
            )

            OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sa_reset))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DisclosureCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(stringResource(R.string.sa_what), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.sa_privacy), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.sa_not_safety),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                stringResource(R.string.sa_battery),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MicCard(micGranted: Boolean, onRequest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.sa_mic),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (micGranted) {
                Text(stringResource(R.string.sa_mic_granted), style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sa_mic_grant))
                }
            }
        }
    }
}

@Composable
private fun ListenCard(
    listening: Boolean,
    micGranted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.sa_listen),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(if (listening) R.string.sa_listening else R.string.sa_stopped),
                style = MaterialTheme.typography.bodyMedium
            )
            if (listening) {
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sa_stop))
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = micGranted,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.sa_start)) }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: SoundCategory,
    enabled: Boolean,
    sensitivity: Sensitivity,
    channel: AlertChannel,
    hasFlash: Boolean,
    onToggle: (Boolean) -> Unit,
    onSensitivity: (Sensitivity) -> Unit,
    onChannel: (AlertChannel) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(category.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(category.reliabilityRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled) {
                LabeledSegmented(
                    label = stringResource(R.string.sa_sensitivity),
                    options = Sensitivity.entries,
                    selected = sensitivity,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = onSensitivity
                )
                ChannelSelector(
                    hasFlash = hasFlash,
                    channel = channel,
                    onChannel = onChannel
                )
            }
        }
    }
}

@Composable
private fun ChannelSelector(
    hasFlash: Boolean,
    channel: AlertChannel,
    onChannel: (AlertChannel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Text(
            stringResource(R.string.sa_how),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasFlash) {
            SegmentedRow(
                options = AlertChannel.entries,
                selected = channel,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onChannel
            )
        } else {
            Text(
                stringResource(R.string.sa_no_flash),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> LabeledSegmented(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedRow(options = options, selected = selected, optionLabel = optionLabel, onSelect = onSelect)
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}
            ) {
                Text(optionLabel(option), maxLines = 1)
            }
        }
    }
}

@StringRes
private fun SoundCategory.reliabilityRes(): Int = when (reliability) {
    SoundReliability.ALTA -> R.string.sa_reliab_high
    SoundReliability.MEDIA -> R.string.sa_reliab_med
}

@StringRes
private fun Sensitivity.labelRes(): Int = when (this) {
    Sensitivity.BAJA -> R.string.sa_sens_low
    Sensitivity.MEDIA -> R.string.sa_sens_med
    Sensitivity.ALTA -> R.string.sa_sens_high
}

@StringRes
private fun AlertChannel.labelRes(): Int = when (this) {
    AlertChannel.FLASH -> R.string.sa_chan_flash
    AlertChannel.PANTALLA -> R.string.sa_chan_screen
    AlertChannel.AMBAS -> R.string.sa_chan_both
}
