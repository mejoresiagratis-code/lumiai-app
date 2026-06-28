package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.mejoresiagratis.lumiai.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lote 6 (accesibilidad) — verificación de `semantics` del orbe de encendido.
 *
 * Corre en JVM mediante Robolectric (tarea `testDebugUnitTest`), sin emulador, de modo que CI
 * lo compila y ejecuta. Se desactiva el auto-avance del reloj para no bloquear con las
 * animaciones infinitas del orbe; los aserts leen el árbol de semántica directamente.
 *
 * Las etiquetas se leen de los MISMOS recursos que usa el composable (a11y_torch / a11y_state_on /
 * a11y_state_off), no de literales: así el test es independiente del idioma por defecto (la app es
 * bilingüe EN/ES) y de cambios de redacción.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PowerOrbSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val res get() = RuntimeEnvironment.getApplication()
    private val torchLabel get() = res.getString(R.string.a11y_torch)
    private val onLabel get() = res.getString(R.string.a11y_state_on)
    private val offLabel get() = res.getString(R.string.a11y_state_off)

    private fun stateMatcher(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun orb_apagado_expone_estado_apagada() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = false, onToggle = {}) }
        }
        composeRule.onNodeWithContentDescription(torchLabel)
            .assert(stateMatcher(offLabel))
    }

    @Test
    fun orb_encendido_expone_estado_encendida() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = true, onToggle = {}) }
        }
        composeRule.onNodeWithContentDescription(torchLabel)
            .assert(stateMatcher(onLabel))
    }

    @Test
    fun orb_click_invoca_toggle() {
        composeRule.mainClock.autoAdvance = false
        var toggled = false
        composeRule.setContent {
            MaterialTheme { PowerOrb(isOn = false, onToggle = { toggled = true }) }
        }
        composeRule.onNodeWithContentDescription(torchLabel).performClick()
        assertTrue(toggled)
    }
}
