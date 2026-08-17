package com.mejoresiagratis.lumiai.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

/**
 * Dialogo de marca (sustituye al AlertDialog plano de M3 en los pop-ups clave):
 * tarjeta 28 dp con sombra, insignia circular con icono sobre primaryContainer,
 * titulo y cuerpo centrados y botonera VERTICAL de ancho completo (accion primaria
 * rellena, secundaria delineada, cierre en texto). Todos los colores por roles de
 * colorScheme: temas y acentos aplican solos.
 */
@Composable
fun LumiDialog(
    onDismiss: () -> Unit,
    @DrawableRes iconRes: Int,
    title: String,
    /** Cuerpo simple. Se ignora si se pasa [bodyContent]. */
    body: String? = null,
    /**
     * Cuerpo con formato propio (16-ago): lo usa Novedades, que necesita versiones en negrita,
     * alineado a la izquierda y aire entre entradas. Con `body` plano no se puede expresar eso
     * y duplicar el diálogo entero por un caso sería peor. Si viene, sustituye a [body].
     */
    bodyContent: (@Composable ColumnScope.() -> Unit)? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    dismissLabel: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = LumiSpacing.lg,
                    vertical = LumiSpacing.lg
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (bodyContent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(LumiSpacing.md)
                    ) { bodyContent() }
                } else if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    // Cuerpo desplazable (16-ago): el diálogo de Novedades crece con cada
                    // versión y en pantallas pequeñas se cortaba. Con heightIn(max) el scroll
                    // solo entra en juego cuando el texto no cabe — los diálogos cortos, que
                    // son la mayoría, se ven exactamente igual que antes.
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LumiSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(LumiSpacing.xs)
                ) {
                    if (primaryLabel != null && onPrimary != null) {
                        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
                            Text(primaryLabel)
                        }
                    }
                    if (secondaryLabel != null && onSecondary != null) {
                        OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                            Text(secondaryLabel)
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(dismissLabel) }
                }
            }
        }
    }
}
