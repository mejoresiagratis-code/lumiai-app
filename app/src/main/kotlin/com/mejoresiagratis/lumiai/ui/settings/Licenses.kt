package com.mejoresiagratis.lumiai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.R

/**
 * Licencias de código abierto (17-ago). Sustituye al marcador "próximamente".
 *
 * TODAS las licencias de esta lista se verificaron consultando el POM publicado de cada
 * artefacto, no de memoria — y ahí saltó una sorpresa: MaterialKolor es MIT, no Apache 2.0
 * como el resto. Suponerlo habría metido un dato legal falso en la app.
 *
 * Se listan las dependencias DIRECTAS del proyecto. Las bibliotecas de Google que se
 * distribuyen bajo los términos del SDK de Android (Play Billing, Anuncios, Mensajes al
 * usuario, Identidad) no son código abierto y se agrupan aparte, indicándolo con claridad.
 */
private data class LicenseEntry(
    val library: String,
    val license: String
)

private val APACHE = "Apache License 2.0"
private val MIT = "MIT License"
private val ANDROID_SDK = "Android Software Development Kit License"

private val LICENSES = listOf(
    LicenseEntry("AndroidX (Core, Activity, Lifecycle, Navigation, DataStore, Credentials, SplashScreen)", APACHE),
    LicenseEntry("Jetpack Compose · Material 3", APACHE),
    LicenseEntry("Kotlin · kotlinx.coroutines", APACHE),
    LicenseEntry("Dagger Hilt", APACHE),
    LicenseEntry("Firebase (Auth, Firestore, Crashlytics, App Check)", APACHE),
    LicenseEntry("MediaPipe Tasks Audio · YAMNet", APACHE),
    LicenseEntry("Coil", APACHE),
    LicenseEntry("Haze — Chris Banes", APACHE),
    LicenseEntry("MaterialKolor — Jordon de Hoog", MIT),
    LicenseEntry("Google Play Billing · Anuncios · Mensajes al usuario · Identidad", ANDROID_SDK)
)

/** Cuerpo del diálogo de licencias: biblioteca en negrita, licencia debajo, con separación. */
@Composable
fun ColumnScope.LicensesContent() {
    Text(
        text = stringResource(R.string.licenses_intro),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    LICENSES.forEach { entry ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = entry.library,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                text = entry.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Text(
        text = stringResource(R.string.licenses_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}
