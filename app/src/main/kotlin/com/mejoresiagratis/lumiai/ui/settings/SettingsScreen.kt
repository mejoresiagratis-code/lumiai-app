package com.mejoresiagratis.lumiai.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.ui.theme.solidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    accentColor: AccentColor,
    onSelectAccent: (AccentColor) -> Unit,
    onOpenAuth: () -> Unit,
    onBack: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val user by accountViewModel.user.collectAsStateWithLifecycle()
    val isGuest = user == null || user?.isAnonymous == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .padding(horizontal = LumiSpacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Cuenta ---
            Text(
                text = stringResource(R.string.account_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = LumiSpacing.md)
            )
            if (isGuest) {
                Text(
                    text = stringResource(R.string.account_guest),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.account_guest_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = LumiSpacing.xs, bottom = LumiSpacing.sm)
                )
                Button(
                    onClick = onOpenAuth,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.account_sign_in)) }
            } else {
                Text(
                    text = stringResource(
                        R.string.account_signed_in_as,
                        user?.email ?: user?.uid.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = LumiSpacing.sm)
                )
                OutlinedButton(
                    onClick = { accountViewModel.signOut() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.account_sign_out)) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = LumiSpacing.lg))

            // --- Tema ---
            Text(
                text = stringResource(R.string.theme_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = LumiSpacing.sm)
            )
            ThemeOption(R.string.theme_system, themeMode == ThemeMode.SYSTEM) { onSelectTheme(ThemeMode.SYSTEM) }
            ThemeOption(R.string.theme_light, themeMode == ThemeMode.LIGHT) { onSelectTheme(ThemeMode.LIGHT) }
            ThemeOption(R.string.theme_dark, themeMode == ThemeMode.DARK) { onSelectTheme(ThemeMode.DARK) }

            HorizontalDivider(modifier = Modifier.padding(vertical = LumiSpacing.lg))

            // --- Apariencia: color de acento ---
            Text(
                text = stringResource(R.string.accent_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = LumiSpacing.sm)
            )
            AccentColor.entries.forEach { ac ->
                AccentOption(
                    accent = ac,
                    selected = accentColor == ac,
                    onClick = { onSelectAccent(ac) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    @StringRes labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = LumiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccentOption(
    accent: AccentColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = LumiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
    ) {
        RadioButton(selected = selected, onClick = null)
        val swatch = Modifier
            .size(22.dp)
            .clip(CircleShape)
        if (accent == AccentColor.MULTICOLOR) {
            Box(
                swatch.background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFFB300), Color(0xFF4D7BFF), Color(0xFFE12B2B),
                            Color(0xFF9B6CFF), Color(0xFF11A693), Color(0xFFFFB300)
                        )
                    )
                )
            )
        } else {
            Box(swatch.background(accent.solidColor()))
        }
        Text(text = stringResource(accentLabel(accent)), style = MaterialTheme.typography.bodyLarge)
    }
}

@StringRes
private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.MULTICOLOR -> R.string.accent_multicolor
    AccentColor.AMBER -> R.string.accent_amber
    AccentColor.WHITE -> R.string.accent_white
    AccentColor.RED -> R.string.accent_red
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.VIOLET -> R.string.accent_violet
}
