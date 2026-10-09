package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mejoresiagratis.lumiai.data.sound.DataStoreSoundAlertConfigRepository
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.sound.AlertChannel
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DevicePreferencesPersistenceTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `preferences survive datastore reopen but hardware session stays off`() = runTest {
        val file = folder.newFile("settings.preferences_pb")
        val job = Job()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))) { file }
        val flash = DataStoreFlashStateRepository(store)
        flash.updateSettings { it.copy(intensityLevel = 37, morseText = "TEST") }
        flash.setOn(true)
        flash.setMode(FlashMode.STROBE)
        DataStoreThemePreferencesRepository(store).setThemeMode(ThemeMode.DARK)
        val sounds = DataStoreSoundAlertConfigRepository(store)
        sounds.setEnabled(SoundCategory.GATO, true)
        sounds.setSensitivity(SoundCategory.GATO, Sensitivity.ALTA)
        sounds.setChannel(SoundCategory.GATO, AlertChannel.PANTALLA)
        job.cancelAndJoin()

        val nextJob = Job()
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + StandardTestDispatcher(testScheduler))) { file }
            val next = DataStoreFlashStateRepository(reopened)
            assertEquals(37, next.settings.first().intensityLevel)
            assertEquals("TEST", next.settings.first().morseText)
            assertFalse(next.isOn.value)
            assertEquals(FlashMode.CONTINUOUS, next.mode.first())
            assertEquals(ThemeMode.DARK, DataStoreThemePreferencesRepository(reopened).themeMode.first())
            val config = DataStoreSoundAlertConfigRepository(reopened).config.first()
            assertTrue(config.isEnabled(SoundCategory.GATO))
            assertEquals(Sensitivity.ALTA, config.sensitivity(SoundCategory.GATO))
            assertEquals(AlertChannel.PANTALLA, config.channel(SoundCategory.GATO))
        } finally { nextJob.cancelAndJoin() }
    }
}
