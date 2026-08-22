package com.mejoresiagratis.lumiai.ui.settings

import androidx.annotation.StringRes
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
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Novedades (16-ago). Datos ESTRUCTURADOS, no un string gigante con saltos de línea: así cada
 * versión puede llevar su propio formato (negrita, alineación, separación) y añadir una entrada
 * es una línea en esta lista, no editar un párrafo enorme en dos idiomas.
 *
 * CRITERIO DE QUÉ ENTRA (regla de producto de Pablo): solo lo que el usuario nota y le interesa
 * — funciones nuevas, rediseños y cambios en las reglas de acceso. NO entran correcciones de
 * errores ni cambios internos: si el usuario nunca supo que algo estaba roto, enterarse ahora
 * solo resta confianza. El changelog vende el producto, no documenta el desarrollo.
 */
private data class ChangelogEntry(
    val version: String,
    @StringRes val descRes: Int
)

private val CHANGELOG = listOf(
    ChangelogEntry("v0.9.48", R.string.changelog_pro_revoke),
    ChangelogEntry("v0.9.43", R.string.changelog_privacy),
    ChangelogEntry("v0.9.40", R.string.changelog_licenses),
    ChangelogEntry("v0.9.38", R.string.changelog_avatar),
    ChangelogEntry("v0.9.37", R.string.changelog_multicolor_led),
    ChangelogEntry("v0.9.34", R.string.changelog_security),
    ChangelogEntry("v0.9.32", R.string.changelog_legal_inapp),
    ChangelogEntry("v0.9.31", R.string.changelog_redesign),
    ChangelogEntry("v0.9.29", R.string.changelog_screen_mode),
    ChangelogEntry("v0.9.28", R.string.changelog_sound_alert),
    ChangelogEntry("v0.9.20", R.string.changelog_access_theme),
    ChangelogEntry("v0.4.x", R.string.changelog_older)
)

/** Cuerpo del diálogo de Novedades: versión en negrita, texto a la izquierda, aire entre entradas. */
@Composable
fun ColumnScope.ChangelogContent() {
    CHANGELOG.forEach { entry ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = entry.version,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )
            Text(
                text = stringResource(entry.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
