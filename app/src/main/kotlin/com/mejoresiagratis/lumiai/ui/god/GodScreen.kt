package com.mejoresiagratis.lumiai.ui.god

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/** Vista de superusuario (solo debug): fuerza permisos y desbloqueos para QA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodScreen(
    onBack: () -> Unit,
    viewModel: GodViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("God · superusuario (debug)") },
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
            GodGroup("Estado efectivo") {
                Text("Cuenta: ${onOff(ui.effectiveAccount)}", style = MaterialTheme.typography.bodyMedium)
                Text("Suscripción: ${onOff(ui.effectiveSubscription)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Pro temporal: " + if (ui.proActive) "activo · " + formatGod(ui.proRemainingMillis) else "inactivo",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Anuncios: ${ui.adsWatched}/${ui.adsPerGrant}", style = MaterialTheme.typography.bodyMedium)
            }

            GodGroup("Pro temporal") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { viewModel.grantMillis(TemporaryUnlock.HOUR_MS) }) { Text("+1 h") }
                    Button(onClick = { viewModel.grantMillis(5 * 60_000L) }) { Text("+5 min") }
                    OutlinedButton(onClick = { viewModel.clearPro() }) { Text("Borrar") }
                }
            }

            GodGroup("Progreso de anuncios") {
                Row(horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                    OutlinedButton(onClick = { viewModel.setRewardCount(0) }) { Text("0") }
                    OutlinedButton(onClick = { viewModel.setRewardCount(1) }) { Text("1") }
                }
            }

            GodGroup("Forzar cuenta") {
                TriState(current = ui.forceAccount, onChange = viewModel::setForceAccount)
            }
            GodGroup("Forzar suscripción (tier Pro/IA)") {
                TriState(current = ui.forceSubscription, onChange = viewModel::setForceSubscription)
            }

            Button(onClick = { viewModel.resetAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset total")
            }
        }
    }
}

@Composable
private fun GodGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun TriState(current: Boolean?, onChange: (Boolean?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        TriButton("Real", current == null) { onChange(null) }
        TriButton("Sí", current == true) { onChange(true) }
        TriButton("No", current == false) { onChange(false) }
    }
}

@Composable
private fun TriButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

private fun onOff(value: Boolean): String = if (value) "sí" else "no"

private fun formatGod(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60L
    val sec = totalSec % 60L
    return "%02d:%02d".format(m, sec)
}
