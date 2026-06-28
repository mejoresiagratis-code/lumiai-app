package com.mejoresiagratis.lumiai.ui.home.beamhub

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import com.mejoresiagratis.lumiai.ui.home.components.MODE_CATALOG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lote 6 (accesibilidad) — verificación de `semantics` del selector de modo (ModePill).
 *
 * Protege justo el contrato que ya rompió CI una vez (el shadowing de `selected` → `this.selected`
 * y el `stateDescription` de bloqueado). Corre en JVM con Robolectric (`testDebugUnitTest`).
 * Reutiliza `MODE_CATALOG.first()` como fixture, sin construir `FlashMode`/recursos a mano.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModePillSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleMode = MODE_CATALOG.first()

    @Test
    fun pill_no_seleccionado_reporta_selected_false_y_rol_tab() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                ModePill(item = sampleMode, selected = false, locked = false, onClick = {})
            }
        }
        composeRule.onNode(isSelectable())
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test
    fun pill_seleccionado_reporta_selected_true() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                ModePill(item = sampleMode, selected = true, locked = false, onClick = {})
            }
        }
        composeRule.onNode(isSelectable()).assertIsSelected()
    }

    @Test
    fun pill_bloqueado_expone_state_description() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                ModePill(item = sampleMode, selected = false, locked = true, onClick = {})
            }
        }
        composeRule.onNode(isSelectable())
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
    }
}
