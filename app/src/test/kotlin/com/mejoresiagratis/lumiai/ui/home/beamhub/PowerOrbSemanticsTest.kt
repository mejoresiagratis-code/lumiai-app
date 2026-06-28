package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lote 6 (accesibilidad) — verificación de `semantics` del orbe de encendido.
 *
 * Corre en JVM mediante Robolectric (tarea `testDebugUnitTest`), sin emulador, de modo que CI
 * lo compila y ejecuta. Se desactiva el auto-avance del reloj para no bloquear con las
 * animaciones infinitas del orbe; los aserts leen el árbol de semántica directamente.
 *
 * Las etiquetas son las cadenas reales en español (app de un solo idioma): a11y_torch=\"Linterna\",
 * a11y_state_on=\"Encendida\", a11y_state_off=\"Apagada\".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PowerOrbSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun stateMatcher(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun orb_apagado_expone_estado_apagada() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = false, onToggle = {}) }
        }
        composeRule.onNodeWithContentDescription("Linterna")
            .assert(stateMatcher("Apagada"))
    }

    @Test
    fun orb_encendido_expone_estado_encendida() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = true, onToggle = {}) }
        }
        composeRule.onNodeWithContentDescription("Linterna")
            .assert(stateMatcher("Encendida"))
    }

    @Test
    fun orb_click_invoca_toggle() {
        composeRule.mainClock.autoAdvance = false
        var toggled = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = false, onToggle = { toggled = true }) }
        }
        composeRule.onNodeWithContentDescription("Linterna").performClick()
        assertTrue(toggled)
    }
}
