package com.mejoresiagratis.lumiai.ui.settings

import android.app.Activity
import com.mejoresiagratis.lumiai.BuildConfig
import com.mejoresiagratis.lumiai.domain.entitlement.canStartSubscriptionPurchase
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import coil3.compose.AsyncImage
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import com.mejoresiagratis.lumiai.ui.components.LumiDialog
import com.mejoresiagratis.lumiai.ui.god.GodViewModel
import com.mejoresiagratis.lumiai.ui.theme.LumiMotion
import com.mejoresiagratis.lumiai.ui.theme.solidColor
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
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
    onOpenGod: () -> Unit,
    onOpenSoundAlert: () -> Unit,
    onOpenLedBanner: () -> Unit,
    // WebView interno (14-ago): antes Intent.ACTION_VIEW al navegador del sistema, sacaba al
    // usuario de la app — ahora navegación dentro de LumiAI, patrón de la mayoría de apps.
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onBack: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel(),
    rewardedUnlockViewModel: RewardedUnlockViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val user by accountViewModel.user.collectAsStateWithLifecycle()
    val isGuest = user == null || user?.isAnonymous == true
    val accountUi by accountViewModel.ui.collectAsStateWithLifecycle()
    val proUi by rewardedUnlockViewModel.ui.collectAsStateWithLifecycle()
    // A un anuncio del premio, TODAS las superficies deben decirlo — el drawer ya lo hacia
    // pero los dialogos de "Modo bloqueado" iban a ciegas con el copy generico, justo en el
    // momento de maxima intencion (QA de Pablo). Un solo flag para todos.
    val lastAdPending = proUi.adsWatched >= proUi.adsPerGrant - 1 && !proUi.active
    val context = LocalContext.current
    val hasVibrator = remember {
        runCatching { context.getSystemService(Vibrator::class.java)?.hasVibrator() == true }.getOrDefault(false)
    }
    val scope = rememberCoroutineScope()
    val webClientId = accountViewModel.webClientId
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var accentLockDialog by remember { mutableStateOf<AccentLock?>(null) }
    var showSubscribeGate by remember { mutableStateOf(false) }
    var showSoundAlertLocked by remember { mutableStateOf(false) }
    var showLedLocked by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    val billingProfile by accountViewModel.billingProfile.collectAsStateWithLifecycle()
    // El perfil de facturacion se toca una vez y estorba el resto del tiempo: plegado por
    // defecto tras una fila-cabecera con chevron. rememberSaveable para respetar el estado
    // si el proceso muere con el panel abierto.
    var billingOpen by rememberSaveable { mutableStateOf(false) }
    val subscriptionUi by subscriptionViewModel.ui.collectAsStateWithLifecycle()

    // Si el acento persistido quedó bloqueado (p. ej. caducó el Pro con Multicolor,
    // o se cerró sesión con un sólido de cuenta), se vuelve al azul de marca.
    // Usa el acceso Pro EFECTIVO (17-ago), igual que los swatches: con multicolor
    // desbloqueable por anuncios, al agotarse la hora el acento debe revertir solo.
    // La clave del efecto incluye proUnlocked para que ese momento se detecte.
    LaunchedEffect(accentColor, isGuest, proUi.proUnlocked) {
        if (!accentColor.isUnlocked(hasAccount = !isGuest, hasPro = proUi.proUnlocked)) {
            onSelectAccent(AccentColor.BLUE)
        }
    }
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

    LaunchedEffect(subscriptionUi.lastMessage) {
        subscriptionUi.lastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            subscriptionViewModel.consumeMessage()
        }
    }

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
                        Avatar(
                            letter = (email ?: user?.uid.orEmpty()).take(1).uppercase().ifBlank { "?" },
                            photoUrl = user?.photoUrl
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                        ) {
                            // Verificado = solo un check junto al correo (rediseño 14-ago);
                            // el texto completo vive en la semantica para TalkBack. El estado
                            // SIN verificar conserva su pill: es accionable y debe verse.
                            val verifiedCd = stringResource(R.string.account_email_verified)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                            ) {
                                Text(
                                    text = email ?: user?.uid.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (email != null && user?.isEmailVerified == true) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = verifiedCd,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (email != null && user?.isEmailVerified != true) {
                                StatusPill(
                                    iconRes = R.drawable.ic_info,
                                    textRes = R.string.account_email_unverified,
                                    container = MaterialTheme.colorScheme.errorContainer,
                                    onContainer = MaterialTheme.colorScheme.onErrorContainer
                                )
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

                    // ── "Mis datos" (rediseño 14-ago): el perfil de facturacion vive DENTRO
                    // de Cuenta como desplegable, y "Borrar cuenta" se muda a su interior —
                    // accesible pero fuera del primer nivel, como toda accion destructiva. ──
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val chev by animateFloatAsState(
                        targetValue = if (billingOpen) 180f else 0f,
                        animationSpec = LumiMotion.emphasized(),
                        label = "misDatosChevron"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { billingOpen = !billingOpen }
                            .padding(vertical = LumiSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.misdatos_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chev)
                        )
                    }
                    AnimatedVisibility(visible = billingOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                            Text(
                                text = stringResource(R.string.billing_explainer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PersistedTextField(
                                persistedValue = billingProfile.fullName,
                                onPersist = accountViewModel::setFullName,
                                label = stringResource(R.string.billing_full_name)
                            )
                            PersistedTextField(
                                persistedValue = billingProfile.billingCountry,
                                onPersist = accountViewModel::setBillingCountry,
                                label = stringResource(R.string.billing_country)
                            )
                            OutlinedButton(
                                onClick = {
                                    val i = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/account/subscriptions")
                                    )
                                    runCatching { context.startActivity(i) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.billing_payment_method)) }
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
                }
            }

            // --- Tema ---
            // --- Acceso Pro temporal ---
            SettingsSection(R.string.pro_section) {
                // UNA sola linea (rediseño 14-ago, principios del skill de copywriting:
                // ultra-especifico, beneficio primero) — mata la triple redundancia anterior.
                // La barra es el unico indicador de progreso; el boton lleva el contador.
                Text(
                    text = if (proUi.active) {
                        stringResource(R.string.pro_remaining, formatProDuration(proUi.remainingMillis))
                    } else {
                        stringResource(R.string.pro_one_liner)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!proUi.active) {
                    val watched = proUi.adsWatched.coerceIn(0, proUi.adsPerGrant)
                    val total = proUi.adsPerGrant.coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { watched.toFloat() / total.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LumiSpacing.xs)
                    )
                }
                val activity = remember(context) { context.findActivity() }
                val msgGranted = stringResource(R.string.pro_granted)
                val msgUnavailable = stringResource(R.string.pro_ad_unavailable)
                val msgProgressFmt = stringResource(R.string.pro_progress_more)
                // Con Pro activo el boton de anuncios se OCULTA (no solo se deshabilita):
                // el tope canjeable es 1 h y ofrecer mas seria prometer algo falso (QA 13-ago).
                // Ver anuncios exige cuenta CON correo verificado (13-ago): sin eso, el
                // boton se sustituye por el aviso correspondiente en vez de ofrecerlo.
                if (!proUi.active && proUi.canTryPro) {
                    // Jerarquia de CTAs corregida (principio de producto que estaba
                    // invertido en pantalla): tonal = anuncio/herramienta; el relleno
                    // queda reservado para la conversion Pro de abajo.
                    FilledTonalButton(
                        onClick = {
                            val act = activity
                            if (act != null) {
                                rewardedUnlockViewModel.watchAd(
                                    activity = act,
                                    onReward = { outcome ->
                                        val msg = if (outcome.grantsUnlock) {
                                            msgGranted
                                        } else {
                                            val remaining = (RewardProgress.ADS_PER_GRANT - outcome.newCount).coerceAtLeast(1)
                                            msgProgressFmt.format(remaining)
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onUnavailable = {
                                        Toast.makeText(context, msgUnavailable, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = activity != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when {
                                !proUi.adReady -> stringResource(R.string.pro_watch_ad_loading)
                                proUi.adsWatched >= proUi.adsPerGrant - 1 ->
                                    stringResource(R.string.pro_watch_ad_last)
                                else -> stringResource(
                                    R.string.pro_watch_ad_count,
                                    proUi.adsWatched + 1,
                                    proUi.adsPerGrant
                                )
                            }
                        )
                    }
                } else if (!proUi.active && !proUi.hasAccount) {
                    OutlinedButton(
                        onClick = onOpenAuth,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.mode_unlock_sign_in)) }
                } else if (!proUi.active && !proUi.isEmailVerified) {
                    Text(
                        text = stringResource(R.string.mode_locked_ai_verify),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = LumiSpacing.xs)
                    )
                }
                if (!proUi.hasSubscription) {
                    Button(
                        onClick = {
                            // Regla de producto: SUSCRIBIRSE exige cuenta con sesión iniciada
                            // (a diferencia del desbloqueo temporal por anuncios, que no la exige).
                            if (!canStartSubscriptionPurchase(hasAccount = !isGuest)) showSubscribeGate = true else subscriptionViewModel.purchase(context.findActivity())
                        },
                        enabled = !subscriptionUi.purchasing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LumiSpacing.xs)
                    ) {
                        Text(
                            subscriptionUi.product?.let {
                                stringResource(R.string.pro_subscribe_cta_price, it.formattedPrice)
                            } ?: stringResource(R.string.pro_subscribe_cta)
                        )
                    }
                }
            }

            // ── Herramientas Pro (rediseño 14-ago): Alerta Sonora + Letrero LED unidas —
            // compartían patrón idéntico en dos secciones; ahora dos filas compactas,
            // colocadas bajo Acceso Pro a propósito: son lo que el Pro desbloquea. ──
            SettingsSection(R.string.tools_pro_section) {
                ToolRow(
                    titleRes = R.string.sa_title,
                    descRes = R.string.sound_alert_explainer,
                    buttonTextRes = if (proUi.proUnlocked) R.string.action_open else R.string.mode_unlock_watch_ad,
                    onClick = { if (proUi.proUnlocked) onOpenSoundAlert() else showSoundAlertLocked = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToolRow(
                    titleRes = R.string.led_title,
                    descRes = R.string.led_explainer,
                    buttonTextRes = if (proUi.proUnlocked) R.string.action_open else R.string.mode_unlock_watch_ad,
                    onClick = { if (proUi.proUnlocked) onOpenLedBanner() else showLedLocked = true }
                )
            }

            // ── Apariencia (rediseño 14-ago): Tema + Acento + Estilo, antes en 2
            // secciones separadas, ahora una tarjeta con controles etiquetados. ──
            SettingsSection(R.string.appearance_section) {
                Text(
                    text = stringResource(R.string.theme_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemeSegmented(selected = themeMode, onSelect = onSelectTheme)
                Text(
                    text = stringResource(R.string.accent_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccentSwatches(
                    selected = accentColor,
                    hasAccount = !isGuest,
                    // Acceso Pro EFECTIVO (17-ago): incluye el desbloqueo temporal por
                    // anuncios, no solo la suscripción — multicolor se comporta ya como
                    // el resto de herramientas Pro.
                    hasPro = proUi.proUnlocked,
                    onSelect = onSelectAccent,
                    onLockedAccount = { accentLockDialog = AccentLock.ACCOUNT },
                    onLockedPro = { accentLockDialog = AccentLock.PRO }
                )
                Text(
                    text = stringResource(R.string.accent_style_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccentStyleSegmented(selected = accentStyle, onSelect = onSelectAccentStyle)
            }

            // --- Accesibilidad (Capa B) ---
            SettingsSection(R.string.a11y_section) {
                // Plegable, plegada por defecto (rediseño 14-ago): 4 toggles compactos.
                var a11yOpen by remember { mutableStateOf(false) }
                val a11yChev by animateFloatAsState(
                    targetValue = if (a11yOpen) 180f else 0f,
                    animationSpec = LumiMotion.emphasized(),
                    label = "a11yChevron"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { a11yOpen = !a11yOpen }
                        .padding(vertical = LumiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.a11y_expand),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(a11yChev)
                    )
                }
                AnimatedVisibility(visible = a11yOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
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

            // ── General (rediseño 14-ago): Idioma + Acerca de en filas uniformes. ──
            SettingsSection(R.string.general_section) {
                SettingsRow(
                    titleRes = R.string.language_row_title,
                    subtitle = stringResource(R.string.language_row_subtitle),
                    onClick = {
                        val i = Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                            data = Uri.parse("package:" + context.packageName)
                        }
                        runCatching { context.startActivity(i) }
                    }
                )
                SettingsRow(
                    titleRes = R.string.about_version,
                    subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = null
                )
                SettingsRow(
                    titleRes = R.string.about_changelog,
                    onClick = { showChangelog = true }
                )
                SettingsRow(
                    titleRes = R.string.about_rate,
                    onClick = {
                        val uri = Uri.parse("market://details?id=" + context.packageName)
                        val i = Intent(Intent.ACTION_VIEW, uri)
                        val fallback = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
                        )
                        runCatching { context.startActivity(i) }
                            .onFailure { runCatching { context.startActivity(fallback) } }
                    }
                )
            }

            SettingsSection(R.string.legal_section) {
                SettingsRow(
                    titleRes = R.string.legal_privacy,
                    onClick = onOpenPrivacyPolicy
                )
                SettingsRow(
                    titleRes = R.string.legal_terms,
                    onClick = onOpenTerms
                )
                SettingsRow(
                    titleRes = R.string.about_licenses,
                    onClick = { showLicenses = true }
                )
            }

            if (BuildConfig.DEBUG) {
                // BuildConfig.DEBUG es constante en compilación: en release este bloque entero
                // (incluido el ViewModel) se elimina, no solo se oculta.
                val godViewModel: GodViewModel = hiltViewModel()
                val godUi by godViewModel.ui.collectAsStateWithLifecycle()
                val overrideActive = godUi.forceAccount != null || godUi.forceSubscription != null
                Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
                    Text(
                        text = "Superusuario (debug)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = LumiSpacing.md)
                    )
                    // Aviso VISIBLE cuando hay permisos forzados: sin esto es fácil confundir
                    // un override activo con un bug de gating (pasó el 25 jul, ver memo).
                    if (overrideActive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(LumiSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                        ) {
                            Text(
                                text = "Permisos forzados activos",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Los modos de pago están desbloqueados por God mode, no por " +
                                    "tu cuenta real. Solo afecta a builds debug.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { godViewModel.clearOverride() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Restaurar permisos reales") }
                        }
                    }
                    Button(onClick = onOpenGod, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir God mode")
                    }
                }
            }
        }
    }

    accentLockDialog?.let { lock ->
        LumiDialog(
            onDismiss = { accentLockDialog = null },
            iconRes = R.drawable.ic_lock,
            title = stringResource(
                if (lock == AccentLock.PRO) R.string.accent_locked_pro_title
                else R.string.accent_locked_account_title
            ),
            body = stringResource(
                if (lock == AccentLock.PRO) R.string.accent_locked_pro_body
                else R.string.accent_locked_account_body
            ),
            primaryLabel = if (lock == AccentLock.ACCOUNT) stringResource(R.string.accent_locked_sign_in) else null,
            onPrimary = if (lock == AccentLock.ACCOUNT) ({ accentLockDialog = null; onOpenAuth() }) else null,
            dismissLabel = stringResource(R.string.dialog_close)
        )
    }

    if (showSubscribeGate) {
        LumiDialog(
            onDismiss = { showSubscribeGate = false },
            iconRes = R.drawable.ic_lock,
            title = stringResource(R.string.pro_subscribe_gate_title),
            body = stringResource(R.string.pro_subscribe_gate_body),
            primaryLabel = stringResource(R.string.accent_locked_sign_in),
            onPrimary = { showSubscribeGate = false; onOpenAuth() },
            dismissLabel = stringResource(R.string.dialog_close)
        )
    }

    if (showSoundAlertLocked) {
        val soundActivity = remember(context) { context.findActivity() }
        val msgGranted = stringResource(R.string.pro_granted)
        val msgUnavailable = stringResource(R.string.pro_ad_unavailable)
        val msgProgressFmt = stringResource(R.string.pro_progress_more)
        val watchAd: () -> Unit = {
            showSoundAlertLocked = false
            val act = soundActivity
            if (act != null) {
                rewardedUnlockViewModel.watchAd(
                    activity = act,
                    onReward = { outcome ->
                        val msg = if (outcome.grantsUnlock) {
                            msgGranted
                        } else {
                            val remaining = (RewardProgress.ADS_PER_GRANT - outcome.newCount).coerceAtLeast(1)
                            msgProgressFmt.format(remaining)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onUnavailable = {
                        Toast.makeText(context, msgUnavailable, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        when {
            isGuest -> LumiDialog(
                onDismiss = { showSoundAlertLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(R.string.mode_locked_ai_sign_in),
                primaryLabel = stringResource(R.string.mode_unlock_sign_in),
                onPrimary = { showSoundAlertLocked = false; onOpenAuth() },
                dismissLabel = stringResource(R.string.dialog_close)
            )
            !proUi.isEmailVerified -> LumiDialog(
                // Ya estamos en Ajustes: el flujo de reenvio vive en la seccion Cuenta,
                // arriba en esta misma pantalla — sin boton de "accion" que solo cerraria
                // (dismiss-only, mismo patron que el dialogo estricto de Tier.PRO).
                onDismiss = { showSoundAlertLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(R.string.mode_locked_ai_verify),
                dismissLabel = stringResource(R.string.dialog_close)
            )
            else -> LumiDialog(
                onDismiss = { showSoundAlertLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(
                    if (lastAdPending) R.string.pro_progress_one_left else R.string.sound_alert_locked
                ),
                primaryLabel = stringResource(
                    if (lastAdPending) R.string.pro_watch_ad_last else R.string.pro_watch_ad
                ),
                onPrimary = watchAd,
                secondaryLabel = stringResource(R.string.pro_subscribe_cta),
                onSecondary = {
                    showSoundAlertLocked = false
                    if (!canStartSubscriptionPurchase(hasAccount = !isGuest)) showSubscribeGate = true else subscriptionViewModel.purchase(context.findActivity())
                },
                dismissLabel = stringResource(R.string.dialog_close)
            )
        }
    }

    if (showLedLocked) {
        val ledActivity = remember(context) { context.findActivity() }
        val msgGranted = stringResource(R.string.pro_granted)
        val msgUnavailable = stringResource(R.string.pro_ad_unavailable)
        val msgProgressFmt = stringResource(R.string.pro_progress_more)
        val watchAd: () -> Unit = {
            showLedLocked = false
            val act = ledActivity
            if (act != null) {
                rewardedUnlockViewModel.watchAd(
                    activity = act,
                    onReward = { outcome ->
                        val msg = if (outcome.grantsUnlock) {
                            msgGranted
                        } else {
                            val remaining = (RewardProgress.ADS_PER_GRANT - outcome.newCount).coerceAtLeast(1)
                            msgProgressFmt.format(remaining)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onUnavailable = {
                        Toast.makeText(context, msgUnavailable, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        when {
            isGuest -> LumiDialog(
                onDismiss = { showLedLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(R.string.mode_locked_ai_sign_in),
                primaryLabel = stringResource(R.string.mode_unlock_sign_in),
                onPrimary = { showLedLocked = false; onOpenAuth() },
                dismissLabel = stringResource(R.string.dialog_close)
            )
            !proUi.isEmailVerified -> LumiDialog(
                // Ya estamos en Ajustes: el flujo de reenvio vive en la seccion Cuenta,
                // arriba en esta misma pantalla — sin boton de "accion" que solo cerraria.
                onDismiss = { showLedLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(R.string.mode_locked_ai_verify),
                dismissLabel = stringResource(R.string.dialog_close)
            )
            else -> LumiDialog(
                onDismiss = { showLedLocked = false },
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.mode_locked_title),
                body = stringResource(
                    if (lastAdPending) R.string.pro_progress_one_left else R.string.led_locked
                ),
                primaryLabel = stringResource(
                    if (lastAdPending) R.string.pro_watch_ad_last else R.string.pro_watch_ad
                ),
                onPrimary = watchAd,
                secondaryLabel = stringResource(R.string.pro_subscribe_cta),
                onSecondary = {
                    showLedLocked = false
                    if (!canStartSubscriptionPurchase(hasAccount = !isGuest)) showSubscribeGate = true else subscriptionViewModel.purchase(context.findActivity())
                },
                dismissLabel = stringResource(R.string.dialog_close)
            )
        }
    }

    if (showChangelog) {
        LumiDialog(
            onDismiss = { showChangelog = false },
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.about_changelog),
            // Cuerpo con formato propio: versión en negrita, texto a la izquierda y aire
            // entre entradas (16-ago). La lista vive en Changelog.kt.
            bodyContent = { ChangelogContent() },
            dismissLabel = stringResource(R.string.dialog_close)
        )
    }

    if (showLicenses) {
        LumiDialog(
            onDismiss = { showLicenses = false },
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.about_licenses),
            // Lista real (17-ago): sustituye al marcador "próximamente". Cada licencia se
            // verificó en el POM publicado del artefacto, no de memoria.
            bodyContent = { LicensesContent() },
            dismissLabel = stringResource(R.string.dialog_close)
        )
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
            text = stringResource(headerRes).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            // 2sp de tracking hacia flotar las cabeceras; 1.2sp mantiene el caracter
            // de overline sin desligar las letras.
            letterSpacing = 1.2.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .padding(start = LumiSpacing.md)
                .semantics { heading() }
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
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

/**
 * Avatar de la cuenta. Si el proveedor aporta foto (Google) se carga desde su URL (17-ago);
 * si no hay foto, o falla la descarga, o aún está cargando, se mantiene la inicial de siempre.
 * La inicial se pinta SIEMPRE detrás: así el círculo nunca aparece vacío mientras llega la
 * imagen, y una caída de red degrada a lo que había antes en vez de a un hueco gris.
 */
@Composable
private fun Avatar(letter: String, photoUrl: String? = null) {
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
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }
    }
}

/** Fila de ajuste simple: icono opcional, título, subtítulo y chevron si es accionable. */
/**
 * Campo de texto cuyo estado de edición es LOCAL y síncrono (un [TextFieldValue] que preserva
 * cursor y selección), y que solo persiste al almacenamiento al perder el foco. Evita cablear el
 * `value` del TextField directamente a un Flow/DataStore asíncrono: eso reordena las escrituras y
 * pierde la posición del cursor, produciendo texto invertido ("Pablo" -> "abloP").
 *
 * - Mientras el usuario escribe: solo se actualiza el estado local -> tecleo instantáneo, cursor OK.
 * - Al perder el foco: se llama a [onPersist] una sola vez, y solo si el texto cambió.
 * - Si el valor persistido cambia desde fuera MIENTRAS el campo no tiene el foco (p. ej. otro
 *   dispositivo, o la carga inicial de DataStore), se re-sincroniza el estado local. Nunca se
 *   pisa lo que el usuario está escribiendo (solo se sincroniza sin foco).
 */
@Composable
private fun PersistedTextField(
    persistedValue: String,
    onPersist: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var fieldState by remember { mutableStateOf(TextFieldValue(persistedValue)) }
    var hasFocus by remember { mutableStateOf(false) }

    // Re-sincroniza SOLO cuando el campo no tiene foco, para no pisar la edición en curso.
    LaunchedEffect(persistedValue, hasFocus) {
        if (!hasFocus && persistedValue != fieldState.text) {
            fieldState = TextFieldValue(persistedValue)
        }
    }

    OutlinedTextField(
        value = fieldState,
        onValueChange = { fieldState = it },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (hasFocus && !focus.isFocused && fieldState.text != persistedValue) {
                    onPersist(fieldState.text)
                }
                hasFocus = focus.isFocused
            }
    )
}

@Composable
private fun SettingsRow(
    @StringRes titleRes: Int,
    subtitle: String? = null,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = LumiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(-90f)
            )
        }
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            // bodyMedium (14/20) hacia que cada descripcion ocupara 3 lineas y el switch
            // quedara flotando; bodySmall con 16sp de interlinea compacta sin perder lectura.
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 16.sp,
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

/** Motivo de bloqueo de un swatch de acento (para elegir el diálogo). */
private enum class AccentLock { ACCOUNT, PRO }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(
    selected: AccentColor,
    hasAccount: Boolean,
    hasPro: Boolean,
    onSelect: (AccentColor) -> Unit,
    onLockedAccount: () -> Unit,
    onLockedPro: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LumiSpacing.md)
        ) {
            AccentColor.entries.forEach { ac ->
                val locked = !ac.isUnlocked(hasAccount = hasAccount, hasPro = hasPro)
                AccentSwatch(
                    accent = ac,
                    selected = selected == ac,
                    locked = locked,
                    onClick = {
                        when {
                            !locked -> onSelect(ac)
                            ac.requiresPro -> onLockedPro()
                            else -> onLockedAccount()
                        }
                    }
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
    locked: Boolean,
    onClick: () -> Unit
) {
    val label = if (locked) {
        stringResource(R.string.accent_locked_cd, stringResource(accentLabel(accent)))
    } else {
        stringResource(accentLabel(accent))
    }
    val ring = MaterialTheme.colorScheme.onSurface
    val swatchScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = LumiMotion.emphasized(),
        label = "swatchScale"
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(swatchScale)
            .alpha(if (locked) 0.55f else 1f)
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
        if (locked) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@StringRes
private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.ORANGE -> R.string.accent_orange
    AccentColor.AMBER -> R.string.accent_amber
    AccentColor.YELLOW -> R.string.accent_yellow
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.RED -> R.string.accent_red
    AccentColor.VIOLET -> R.string.accent_violet
    AccentColor.WHITE -> R.string.accent_white
    AccentColor.MULTICOLOR -> R.string.accent_multicolor
}

@Composable
private fun accountErrorMessage(error: AuthError): String = when (error) {
    AuthError.InvalidCredentials -> stringResource(R.string.auth_error_invalid)
    AuthError.EmailInUse -> stringResource(R.string.auth_error_email_in_use)
    AuthError.WeakPassword -> stringResource(R.string.auth_error_weak_password)
    AuthError.Network -> stringResource(R.string.auth_error_network)
    AuthError.RecentLoginRequired -> stringResource(R.string.auth_error_generic)
    AuthError.GoogleSignInFailed -> stringResource(R.string.auth_error_google)
    AuthError.Unknown -> stringResource(R.string.auth_error_generic)
}

/**
 * Fila compacta de herramienta Pro (rediseño 14-ago): titulo + descripcion de una linea
 * a la izquierda, boton tonal a la derecha — dos secciones enteras reducidas a dos filas.
 */
@Composable
private fun ToolRow(
    titleRes: Int,
    descRes: Int,
    buttonTextRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumiSpacing.md)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(onClick = onClick) { Text(stringResource(buttonTextRes)) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatProDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val sec = totalSec % 60L
    return if (h > 0L) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
