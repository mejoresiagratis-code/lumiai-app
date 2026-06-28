package com.mejoresiagratis.lumiai.ui.sound

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.mejoresiagratis.lumiai.data.sound.SoundAlertService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundReliability
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Pantalla del modo Alerta Sonora (beta, accesible solo en debug por ahora).
 * Muestra la divulgacion destacada (que hace, en el dispositivo, no es seguridad de vida,
 * bateria), pide el permiso de microfono y permite activar/ajustar las 8 categorias.
 * Sin runtime de IA todavia: aqui no se escucha nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundAlertScreen(
    onBack: () -> Unit,
    viewModel: SoundAlertViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
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
                title = { Text("Alerta sonora (beta)") },
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
            // --- Divulgacion destacada ---
            Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                Text(
                    "Qué hace",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Escucha el micrófono y avisa con destellos de luz cuando reconoce un sonido " +
                        "importante. Pensado para no perderte avisos si no puedes oírlos.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Todo ocurre en el dispositivo: no se graba audio ni se envía nada a internet.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "No es un sistema de seguridad. La detección es aproximada y puede fallar; no " +
                        "sustituye a un detector de humo/CO homologado ni a sus avisos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "Escuchar de forma continua consume batería; se recomienda con el móvil cargando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Permiso de microfono ---
            Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                Text(
                    "Micrófono",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (micGranted) "Permiso concedido." else "Permiso necesario para escuchar.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!micGranted) {
                    Button(
                        onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Conceder micrófono")
                    }
                }
            }

            HorizontalDivider()

            // --- Categorias ---
            Text(
                "Sonidos a vigilar",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            SoundCategory.entries.forEach { category ->
                CategoryRow(
                    category = category,
                    enabled = config.isEnabled(category),
                    sensitivity = config.sensitivity(category),
                    onToggle = { viewModel.setEnabled(category, it) },
                    onSensitivity = { viewModel.setSensitivity(category, it) }
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                Text(
                    "Escucha",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (listening) "Escuchando… revisa la notificación." else "Detenido.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (listening) {
                    OutlinedButton(
                        onClick = { SoundAlertService.stop(context); listening = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Parar escucha") }
                } else {
                    Button(
                        onClick = { SoundAlertService.start(context); listening = true },
                        enabled = micGranted,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Iniciar escucha") }
                }
                Text(
                    "Nota: requiere el modelo yamnet.tflite en assets. Sin él no detectará nada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer ajustes")
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: SoundCategory,
    enabled: Boolean,
    sensitivity: Sensitivity,
    onToggle: (Boolean) -> Unit,
    onSensitivity: (Sensitivity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    category.reliabilityLabel() + if (category.safetyRelated) " · no es seguridad de vida" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                Sensitivity.entries.forEach { level ->
                    SensitivityChip(
                        label = level.displayName(),
                        selected = level == sensitivity,
                        onClick = { onSensitivity(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SensitivityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

private fun SoundCategory.displayName(): String = when (this) {
    SoundCategory.TIMBRE -> "Timbre"
    SoundCategory.GOLPES_PUERTA -> "Golpes en la puerta"
    SoundCategory.TELEFONO -> "Teléfono"
    SoundCategory.PERRO -> "Perro"
    SoundCategory.BEBE -> "Llanto de bebé"
    SoundCategory.DESPERTADOR -> "Despertador / alarma"
    SoundCategory.SIRENA -> "Sirena"
    SoundCategory.ALARMA_HUMO -> "Alarma de humo/incendio"
}

private fun SoundCategory.reliabilityLabel(): String = when (reliability) {
    SoundReliability.ALTA -> "fiabilidad alta"
    SoundReliability.MEDIA -> "fiabilidad media"
}

private fun Sensitivity.displayName(): String = when (this) {
    Sensitivity.BAJA -> "Baja"
    Sensitivity.MEDIA -> "Media"
    Sensitivity.ALTA -> "Alta"
}
