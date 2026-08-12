package com.mejoresiagratis.lumiai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.navigation.LumiAiNavHost
import com.mejoresiagratis.lumiai.ui.navigation.Routes
import com.mejoresiagratis.lumiai.ui.start.StartViewModel
import com.mejoresiagratis.lumiai.ui.theme.LocalAutoLockScreen
import com.mejoresiagratis.lumiai.ui.theme.LocalHapticsEnabled
import com.mejoresiagratis.lumiai.ui.theme.LumiAiTheme
import com.mejoresiagratis.lumiai.ui.theme.ThemeViewModel
import com.mejoresiagratis.lumiai.ads.AdsConsentManager
import com.mejoresiagratis.lumiai.ads.RewardedAdController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val startViewModel: StartViewModel by viewModels()

    @Inject lateinit var adsConsentManager: AdsConsentManager
    @Inject lateinit var rewardedAdController: RewardedAdController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { startViewModel.onboardingCompleted.value == null }
        enableEdgeToEdge()
        setContent { LumiAiApp() }

        // Consentimiento (UMP) antes de cualquier anuncio; AdMob solo se inicializa si se permite.
        adsConsentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) rewardedAdController.initializeAndPreload()
        }
    }
}

@Composable
private fun LumiAiApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startViewModel: StartViewModel = hiltViewModel()
) {
    val themeMode: ThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val accent: AccentColor by themeViewModel.accentColor.collectAsStateWithLifecycle()
    val accentStyle: AccentStyle by themeViewModel.accentStyle.collectAsStateWithLifecycle()
    val highContrast: Boolean by themeViewModel.highContrast.collectAsStateWithLifecycle()
    val reduceMotion: Boolean by themeViewModel.reduceMotion.collectAsStateWithLifecycle()
    val haptics: Boolean by themeViewModel.haptics.collectAsStateWithLifecycle()
    val autoLockScreen: Boolean by themeViewModel.autoLockScreen.collectAsStateWithLifecycle()
    val activeMode: FlashMode by themeViewModel.currentMode.collectAsStateWithLifecycle()
    val completed: Boolean? by startViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    LumiAiTheme(
        themeMode = themeMode,
        accent = accent,
        accentStyle = accentStyle,
        highContrast = highContrast,
        reduceMotion = reduceMotion,
        activeMode = activeMode
    ) {
        CompositionLocalProvider(
            LocalHapticsEnabled provides haptics,
            LocalAutoLockScreen provides autoLockScreen
        ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val start = when (completed) {
                null -> null
                true -> Routes.HOME
                false -> Routes.ONBOARDING
            }
            if (start != null) {
                LumiAiNavHost(
                    startDestination = start,
                    themeMode = themeMode,
                    onSelectTheme = themeViewModel::setMode,
                    accentColor = accent,
                    onSelectAccent = themeViewModel::setAccent,
                    accentStyle = accentStyle,
                    onSelectAccentStyle = themeViewModel::setAccentStyle,
                    reduceMotion = reduceMotion,
                    onSetReduceMotion = themeViewModel::setReduceMotion,
                    highContrast = highContrast,
                    onSetHighContrast = themeViewModel::setHighContrast,
                    haptics = haptics,
                    onSetHaptics = themeViewModel::setHaptics,
                    autoLockScreen = autoLockScreen,
                    onSetAutoLockScreen = themeViewModel::setAutoLockScreen
                )
            }
        }
        }
    }
}
