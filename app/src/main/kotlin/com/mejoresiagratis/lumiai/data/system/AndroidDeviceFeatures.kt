package com.mejoresiagratis.lumiai.data.system

import android.content.Context
import android.content.pm.PackageManager
import com.mejoresiagratis.lumiai.domain.capability.DeviceFeatures
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lee las capacidades reales del dispositivo desde PackageManager. */
@Singleton
class AndroidDeviceFeatures @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceFeatures {

    override val hasMicrophone: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
}
