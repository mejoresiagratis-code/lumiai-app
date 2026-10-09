package com.mejoresiagratis.lumiai.domain.sound

import org.junit.Assert.*
import org.junit.Test

class SoundCatalogRegressionTest {
    @Test fun oldSettingsSurviveAndNewSoundsStayOff() {
        val oldCategories = SoundCategory.entries.take(8)
        val raw = oldCategories.joinToString(";") { "${it.name}:0:BAJA:PANTALLA" }
        val restored = SoundAlertConfigCodec.decode(raw)
        oldCategories.forEach {
            assertFalse(restored.isEnabled(it))
            assertEquals(Sensitivity.BAJA, restored.sensitivity(it))
            assertEquals(AlertChannel.PANTALLA, restored.channel(it))
        }
        SoundCategory.entries.drop(8).forEach { assertFalse(restored.isEnabled(it)) }
        assertFalse(restored.anyEnabled)
    }

    @Test fun newSettingsRoundTripIncludingOutputs() {
        val original = SoundAlertConfig().withEnabled(SoundCategory.GATO, true)
            .withSensitivity(SoundCategory.GATO, Sensitivity.BAJA)
            .withChannel(SoundCategory.GATO, AlertChannel.AMBAS)
        assertEquals(original, SoundAlertConfigCodec.decode(SoundAlertConfigCodec.encode(original)))
    }

    @Test fun generalLabelsDoNotPretendToBeSpecific() {
        val matcher = SoundLabelMatcher()
        assertEquals(SoundCategory.LLANTO_GENERAL, matcher.categoryFor("Crying, sobbing"))
        assertEquals(SoundCategory.ALARMA_GENERAL, matcher.categoryFor("Alarm"))
        assertNull(matcher.categoryFor("Emergency vehicle"))
        val engine = SoundDetectionEngine(SoundAlertConfig(), debounceWindows = 1)
        assertTrue(engine.onWindow(mapOf("Crying, sobbing" to 0.99f, "Alarm" to 0.99f), 0).isEmpty())
    }

    @Test fun newSoundsOnlyAlertAfterOptIn() {
        for (category in SoundCategory.entries.drop(8)) {
            val label = category.labels.first()
            assertTrue(SoundDetectionEngine(SoundAlertConfig(), debounceWindows = 1)
                .onWindow(mapOf(label to 0.99f), 0).isEmpty())
            val enabled = SoundAlertConfig().withEnabled(category, true)
            assertEquals(listOf(category), SoundDetectionEngine(enabled, debounceWindows = 1)
                .onWindow(mapOf(label to 0.99f), 0))
        }
    }

    @Test fun specificBabySuppressesGeneralEvenDuringCooldown() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.LLANTO_GENERAL, true)
        val engine = SoundDetectionEngine(cfg, debounceWindows = 1)
        val scores = mapOf("Baby cry, infant cry" to 0.9f, "Crying, sobbing" to 0.9f)
        assertEquals(listOf(SoundCategory.BEBE), engine.onWindow(scores, 0))
        assertTrue(engine.onWindow(scores, 500).isEmpty())
    }

    @Test fun genericAlarmStillWorksWhenSpecificDisabled() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.DESPERTADOR, false)
            .withEnabled(SoundCategory.ALARMA_GENERAL, true)
        assertEquals(listOf(SoundCategory.ALARMA_GENERAL), SoundDetectionEngine(cfg, debounceWindows = 1)
            .onWindow(mapOf("Alarm clock" to 0.9f, "Alarm" to 0.9f), 0))
    }

    @Test fun smokeAlertWinsOverGeneralAndOtherGroupsRemainIndependent() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.ALARMA_GENERAL, true)
        val results = SoundDetectionEngine(cfg, debounceWindows = 1).onWindow(
            mapOf("Fire alarm" to 0.9f, "Alarm" to 0.9f, "Bark" to 0.9f), 0)
        assertEquals(setOf(SoundCategory.ALARMA_HUMO, SoundCategory.PERRO), results.toSet())
    }

    @Test fun generalStreakIsResetWhenSpecificEvidenceArrives() {
        val cfg = SoundAlertConfig().withEnabled(SoundCategory.LLANTO_GENERAL, true)
        val engine = SoundDetectionEngine(cfg)
        assertTrue(engine.onWindow(mapOf("Crying, sobbing" to 0.9f), 0).isEmpty())
        assertTrue(engine.onWindow(mapOf("Crying, sobbing" to 0.9f, "Baby cry, infant cry" to 0.9f), 500).isEmpty())
        assertTrue(engine.onWindow(mapOf("Crying, sobbing" to 0.9f), 1000).isEmpty())
        assertEquals(listOf(SoundCategory.LLANTO_GENERAL), engine.onWindow(mapOf("Crying, sobbing" to 0.9f), 1500))
    }

    @Test fun gunLabelsAreNeverMappedToDoorKnocks() {
        val engine = SoundDetectionEngine(SoundAlertConfig(), debounceWindows = 1)
        assertTrue(engine.onWindow(mapOf("Cap gun" to 0.67f, "Gunshot, gunfire" to 0.33f, "Explosion" to 0.26f), 0).isEmpty())
    }
}
