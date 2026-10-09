package com.mejoresiagratis.lumiai.data.sound

import android.app.Application
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SoundModelMetadataTest {
    @Test fun shippedModelContainsTheExactLabelsUsedByDetection() {
        val context = RuntimeEnvironment.getApplication()
        val model = File.createTempFile("yamnet-label-check", ".tflite", context.cacheDir)
        try {
            context.assets.open(MediaPipeSoundClassifier.YAMNET_MODEL).use { source ->
                model.outputStream().use { source.copyTo(it) }
            }
            ZipFile(model).use { zip ->
                val labels = zip.getInputStream(zip.getEntry("yamnet_label_list.txt"))
                    .bufferedReader().use { it.readLines().toSet() }
                assertEquals(521, labels.size)
                SoundCategory.entries.forEach { category ->
                    assertTrue("Missing packaged labels for $category", labels.containsAll(category.labels))
                }
            }
        } finally { model.delete() }
    }
}
