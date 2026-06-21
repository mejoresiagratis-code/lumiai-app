package com.mejoresiagratis.lumiai.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
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
        ) {
            Text(
                text = stringResource(R.string.theme_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = LumiSpacing.md)
            )
            ThemeOption(R.string.theme_system, themeMode == ThemeMode.SYSTEM) { onSelectTheme(ThemeMode.SYSTEM) }
            ThemeOption(R.string.theme_light, themeMode == ThemeMode.LIGHT) { onSelectTheme(ThemeMode.LIGHT) }
            ThemeOption(R.string.theme_dark, themeMode == ThemeMode.DARK) { onSelectTheme(ThemeMode.DARK) }
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
