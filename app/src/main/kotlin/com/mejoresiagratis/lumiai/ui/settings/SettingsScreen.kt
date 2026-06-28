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
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mejoresiagratis.lumiai.domain.model.AuthError
import kotlinx.coroutines.launch

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
    val accountUi by accountViewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                    modifier = Modifier.padding(bottom = LumiSpacing.xs)
                )
                if (user?.email != null) {
                    if (user?.isEmailVerified == true) {
                        Text(
                            text = stringResource(R.string.account_email_verified),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = LumiSpacing.sm)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.account_email_unverified),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { accountViewModel.resendVerification() },
                            enabled = !accountUi.working,
                            modifier = Modifier
                                .padding(top = LumiSpacing.xs, bottom = LumiSpacing.sm)
                                .heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.account_resend_verification)) }
                    }
                }
                if (accountUi.verificationSent) {
                    Text(
                        text = stringResource(R.string.account_verification_sent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = LumiSpacing.sm)
                    )
                }
                accountUi.error?.let { err ->
                    Text(
                        text = accountErrorMessage(err),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = LumiSpacing.sm)
                    )
                }
                OutlinedButton(
                    onClick = { accountViewModel.signOut() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.account_sign_out)) }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .padding(top = LumiSpacing.xs)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_delete),
                        color = MaterialTheme.colorScheme.error
                    )
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

@Composable
private fun accountErrorMessage(error: AuthError): String = when (error) {
    AuthError.InvalidCredentials -> stringResource(R.string.auth_error_invalid)
    AuthError.EmailInUse -> stringResource(R.string.auth_error_email_in_use)
    AuthError.WeakPassword -> stringResource(R.string.auth_error_weak_password)
    AuthError.Network -> stringResource(R.string.auth_error_network)
    AuthError.RecentLoginRequired -> stringResource(R.string.auth_error_generic)
    AuthError.Unknown -> stringResource(R.string.auth_error_generic)
}
