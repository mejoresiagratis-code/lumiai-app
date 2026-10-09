package com.mejoresiagratis.lumiai.ui.sound

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.mejoresiagratis.lumiai.domain.sound.*
import com.mejoresiagratis.lumiai.testing.SlowTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en-rUS-w480dp-h1200dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SoundCatalogTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun openingAGroupDoesNotChangeSettings() {
        var changes = 0
        composeRule.setContent {
            MaterialTheme {
                SoundCatalog(SoundAlertConfig(), true, { _, _ -> changes++ }, { _, _ -> changes++ }, { _, _ -> changes++ })
            }
        }
        composeRule.onNodeWithText("Doorbell").assertExists()
        composeRule.onNodeWithText("Glass breaking").assertDoesNotExist()
        composeRule.onNodeWithText("Impacts and breakage").performClick()
        composeRule.onNodeWithText("Glass breaking").assertExists()
        composeRule.onNodeWithText("Doorbell").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun newCategorySwitchSelectsOnlyThatCategory() {
        val config = mutableStateOf(SoundAlertConfig())
        var changed: SoundCategory? = null
        composeRule.setContent {
            MaterialTheme {
                SoundCatalog(config.value, true, { category, enabled ->
                    changed = category
                    config.value = config.value.withEnabled(category, enabled)
                }, { _, _ -> }, { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Impacts and breakage").performClick()
        composeRule.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        assertEquals(SoundCategory.CRISTAL_ROTO, changed)
        assertEquals(9, SoundCategory.entries.count { config.value.isEnabled(it) })
    }
}
