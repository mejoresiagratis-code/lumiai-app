package com.mejoresiagratis.lumiai.ui.settings

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import com.mejoresiagratis.lumiai.ui.theme.solidColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    accentColor: AccentColor,
    onSelectAccent: (AccentColor) -> Unit,
    accentStyle: AccentStyle,
    onSelectAccentStyle: (AccentStyle) -> Unit,
    reduceMotion: Boolean,
    onSetReduceMotion: (Boolean) -> Unit,
    highContrast: Boolean,
    onSetHighContrast: (Boolean) -> Unit,
    haptics: Boolean,
    onSetHaptics: (Boolean) -> Unit,
    autoLockScreen: Boolean,
    onSetAutoLockScreen: (Boolean) -> Unit,
    onOpenAuth: () -> Unit,
    onBack: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val user by accountViewModel.user.collectAsStateWithLifecycle()
    val isGuest = user == null || user?.isAnonymous == true
    val accountUi by accountViewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasVibrator = remember {
        runCatching { context.getSystemService(Vibrator::class.java)?.hasVibrator() == true }.getOrDefault(false)
    }
    val scope = rememberCoroutineScope()
    val webClientId = accountViewModel.webClientId
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    val launchGoogleReauth: () -> Unit = {
        val id = webClientId
        if (id != null) {
            scope.launch {
                runCatching {
                    val option = GetSignInWithGoogleOption.Builder(id).build()
                    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    val response = CredentialManager.create(context).getCredential(context, request)
                    val cred = response.credential
                    if (cred is CustomCredential &&
                        cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        GoogleIdTokenCredential.createFrom(cred.data).idToken
                    } else {
                        null
                    }
                }.onSuccess { token ->
                    if (token != null) accountViewModel.reauthGoogleAndDelete(token)
                    else accountViewModel.reportReauthFailure()
                }.onFailure { e ->
                    Log.w("LumiAuth", "Google reauth failed", e)
                    accountViewModel.reportReauthFailure()
                }
            }
        }
    }
    LaunchedEffect(Unit) { accountViewModel.refresh() }

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
                .padding(horizontal = LumiSpacing.lg, vertical = LumiSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
        ) {
            // --- Cuenta ---
            SettingsSection(R.string.account_section) {
                if (isGuest) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
                    ) {
                        Avatar(letter = "?")
                        Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)) {
                            Text(
                                text = stringResource(R.string.account_guest),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.account_guest_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onOpenAuth,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.account_sign_in)) }
                } else {
                    val email = user?.email
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
                    ) {
                        Avatar(letter = (email ?: user?.uid.orEmpty()).take(1).uppercase().ifBlank { "?" })
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                        ) {
                            Text(
                                text = email ?: user?.uid.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (email != null) {
                                if (user?.isEmailVerified == true) {
                                    StatusPill(
                                        iconRes = R.drawable.ic_check,
                                        textRes = R.string.account_email_verified,
                                        container = MaterialTheme.colorScheme.secondaryContainer,
                                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    StatusPill(
                                        iconRes = R.drawable.ic_info,
                                        textRes = R.string.account_email_unverified,
                                        container = MaterialTheme.colorScheme.errorContainer,
                                        onContainer = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                    if (email != null && user?.isEmailVerified != true) {
                        OutlinedButton(
                            onClick = { accountViewModel.resendVerification() },
                            enabled = !accountUi.working,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.account_resend_verification)) }
                    }
                    if (accountUi.verificationSent) {
                        Text(
                            text = stringResource(R.string.account_verification_sent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    accountUi.error?.let { err ->
                        Text(
                            text = accountErrorMessage(err),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = { accountViewModel.signOut() },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.account_sign_out)) }
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.account_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // --- Tema ---
            SettingsSection(R.string.theme_section) {
                ThemeSegmented(selected = themeMode, onSelect = onSelectTheme)
            }

            // --- Apariencia: color de acento ---
            SettingsSection(R.string.accent_section) {
                AccentSwatches(selected = accentColor, onSelect = onSelectAccent)
                Text(
                    text = stringResource(R.string.accent_style_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccentStyleSegmented(selected = accentStyle, onSelect = onSelectAccentStyle)
            }

            // --- Accesibilidad (Capa B) ---
            SettingsSection(R.string.a11y_section) {
                SettingsToggle(
                    titleRes = R.string.a11y_reduce_motion_title,
                    descRes = R.string.a11y_reduce_motion_desc,
                    checked = reduceMotion,
                    onCheckedChange = onSetReduceMotion
                )
                SettingsToggle(
                    titleRes = R.string.a11y_high_contrast_title,
                    descRes = R.string.a11y_high_contrast_desc,
                    checked = highContrast,
                    onCheckedChange = onSetHighContrast
                )
                SettingsToggle(
                    titleRes = R.string.a11y_auto_lock_title,
                    descRes = R.string.a11y_auto_lock_desc,
                    checked = autoLockScreen,
                    onCheckedChange = onSetAutoLockScreen
                )
                if (hasVibrator) {
                    SettingsToggle(
                        titleRes = R.string.a11y_haptics_title,
                        descRes = R.string.a11y_haptics_desc,
                        checked = haptics,
                        onCheckedChange = onSetHaptics
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.account_delete_confirm_title)) },
            text = { Text(stringResource(R.string.account_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    accountViewModel.deleteAccount()
                }) {
                    Text(
                        text = stringResource(R.string.account_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.account_delete_cancel))
                }
            }
        )
    }

    if (accountUi.needsReauth) {
        AlertDialog(
            onDismissRequest = { accountViewModel.dismissReauth() },
            title = { Text(stringResource(R.string.account_reauth_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                    Text(stringResource(R.string.account_reauth_body))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text(stringResource(R.string.auth_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    accountUi.error?.let { err ->
                        Text(
                            text = accountErrorMessage(err),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (webClientId != null) {
                        Text(
                            text = stringResource(R.string.account_reauth_google_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = launchGoogleReauth,
                            enabled = !accountUi.working,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.account_reauth_google_action)) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { accountViewModel.reauthPasswordAndDelete(reauthPassword) },
                    enabled = reauthPassword.isNotBlank() && !accountUi.working
                ) {
                    Text(
                        text = stringResource(R.string.account_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { accountViewModel.dismissReauth() }) {
                    Text(stringResource(R.string.account_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    @StringRes headerRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        Text(
            text = stringResource(headerRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = LumiSpacing.md)
                .semantics { heading() }
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(LumiSpacing.md),
                verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm),
                content = content
            )
        }
    }
}

@Composable
private fun Avatar(letter: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatusPill(
    @androidx.annotation.DrawableRes iconRes: Int,
    @StringRes textRes: Int,
    container: Color,
    onContainer: Color
) {
    Surface(shape = RoundedCornerShape(50), color = container) {
        Row(
            modifier = Modifier.padding(horizontal = LumiSpacing.sm, vertical = LumiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.labelMedium,
                color = onContainer
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    @StringRes titleRes: Int,
    @StringRes descRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmented(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val items = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, (mode, labelRes) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
            ) { Text(stringResource(labelRes)) }
        }
    }
}

@Composable
private fun AccentStyleSegmented(
    selected: AccentStyle,
    onSelect: (AccentStyle) -> Unit
) {
    val items = listOf(
        AccentStyle.WARM to R.string.accent_style_warm,
        AccentStyle.VIVID to R.string.accent_style_vivid
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, (style, labelRes) ->
            SegmentedButton(
                selected = selected == style,
                onClick = { onSelect(style) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
            ) { Text(stringResource(labelRes)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.md)
        ) {
            AccentColor.entries.forEach { ac ->
                AccentSwatch(
                    accent = ac,
                    selected = selected == ac,
                    onClick = { onSelect(ac) }
                )
            }
        }
        Text(
            text = stringResource(R.string.accent_selected, stringResource(accentLabel(selected))),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(accentLabel(accent))
    val ring = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(52.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        val circle = Modifier
            .size(44.dp)
            .clip(CircleShape)
        if (accent == AccentColor.MULTICOLOR) {
            Box(
                circle.background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFFB300), Color(0xFF4D7BFF), Color(0xFFE12B2B),
                            Color(0xFF9B6CFF), Color(0xFF11A693), Color(0xFFFFB300)
                        )
                    )
                )
            )
        } else {
            Box(circle.background(accent.solidColor()))
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(width = 2.5.dp, color = ring, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            )
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@StringRes
private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.MULTICOLOR -> R.string.accent_multicolor
    AccentColor.YELLOW -> R.string.accent_yellow
    AccentColor.AMBER -> R.string.accent_amber
    AccentColor.WHITE -> R.string.accent_white
    AccentColor.RED -> R.string.accent_red
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.VIOLET -> R.string.accent_violet
}

@Composable
private fun accountErrorMessage(error: AuthError): String = when (error) {
    AuthError.InvalidCredentials -> stringResource(R.string.auth_error_invalid)
    AuthError.EmailInUse -> stringResource(R.string.auth_error_email_in_use)
    AuthError.WeakPassword -> stringResource(R.string.auth_error_weak_password)
    AuthError.Network -> stringResource(R.string.auth_error_network)
    AuthError.RecentLoginRequired -> stringResource(R.string.auth_error_generic)
    AuthError.Unknown -> stringResource(R.string.auth_error_generic)
}
