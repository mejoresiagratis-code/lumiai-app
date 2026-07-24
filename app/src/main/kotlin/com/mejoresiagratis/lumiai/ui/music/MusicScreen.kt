package com.mejoresiagratis.lumiai.ui.music

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.music.MusicFlashService
import com.mejoresiagratis.lumiai.domain.music.MusicSensitivity
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Pantalla del modo Musica (Pro): divulgacion honesta (DSP, no IA; nada se graba),
 * permiso de microfono, sensibilidad del detector y escucha anclada abajo.
 * Gate estricto por suscripcion tambien aqui (defensa en profundidad ademas del
 * candado de Ajustes). Sin flash el modo no arranca (es un modo de LED).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val hasSubscription by viewModel.hasSubscription.collectAsStateWithLifecycle()
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()
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
                title = { Text(stringResource(R.string.music_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (hasSubscription && hasFlash) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.sm)
                    ) {
                        if (listening) {
                            OutlinedButton(
                                onClick = { MusicFlashService.stop(context); listening = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.music_stop)) }
                            Text(
                                stringResource(R.string.music_listening),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = LumiSpacing.xs)
                            )
                        } else {
                            Button(
                                onClick = { MusicFlashService.start(context); listening = true },
                                enabled = micGranted,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.music_start)) }
                        }
                    }
                }
            }
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
            when {
                !hasSubscription -> LockedCard()
                !hasFlash -> NoticeCard(R.string.music_needs_flash)
                else -> {
                    DisclosureCard()
                    if (!micGranted) {
                        MicCard(onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) })
                    }
                    SensitivityCard(
                        selected = sensitivity,
                        onSelect = viewModel::setSensitivity
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedCard() {
    Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.music_locked_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.music_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoticeCard(@StringRes textRes: Int) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(LumiSpacing.md)
        )
    }
}

@Composable
private fun DisclosureCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(stringResource(R.string.music_what), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.music_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.strobe_photosensitivity_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MicCard(onRequest: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.sa_mic),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sa_mic_grant))
            }
        }
    }
}

@Composable
private fun SensitivityCard(
    selected: MusicSensitivity,
    onSelect: (MusicSensitivity) -> Unit
) {
    Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.sa_sensitivity),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.music_sensitivity_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = MusicSensitivity.entries
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        icon = {}
                    ) {
                        Text(stringResource(option.labelRes()), maxLines = 1)
                    }
                }
            }
        }
    }
}

@StringRes
private fun MusicSensitivity.labelRes(): Int = when (this) {
    MusicSensitivity.BAJA -> R.string.sa_sens_low
    MusicSensitivity.MEDIA -> R.string.sa_sens_med
    MusicSensitivity.ALTA -> R.string.sa_sens_high
}
