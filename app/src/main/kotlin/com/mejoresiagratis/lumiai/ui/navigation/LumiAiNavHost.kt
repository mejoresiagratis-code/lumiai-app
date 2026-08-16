package com.mejoresiagratis.lumiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.auth.AuthScreen
import com.mejoresiagratis.lumiai.BuildConfig
import com.mejoresiagratis.lumiai.ui.god.GodScreen
import com.mejoresiagratis.lumiai.ui.home.beamhub.BeamHubScreen
import com.mejoresiagratis.lumiai.ui.onboarding.OnboardingScreen
import com.mejoresiagratis.lumiai.ui.settings.SettingsScreen
import com.mejoresiagratis.lumiai.ui.led.LedBannerScreen
import com.mejoresiagratis.lumiai.ui.settings.LegalWebScreen
import com.mejoresiagratis.lumiai.ui.sound.SoundAlertScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val AUTH = "auth"
    const val GOD = "god"
    const val SOUND_ALERT = "sound_alert"
    const val LED_BANNER = "led_banner"
    const val LEGAL_PRIVACY = "legal_privacy"
    const val LEGAL_TERMS = "legal_terms"
}

// URLs propias del documento legal (14-ago): un único punto en todo el código donde viven, para
// que la pantalla de WebView interna y cualquier otro sitio que las necesite lean de aquí.
object LegalUrls {
    const val PRIVACY = "https://mejoresiagratis.com/lumiai/privacy-policy.html"
    const val TERMS = "https://mejoresiagratis.com/lumiai/terms.html"
}

@Composable
fun LumiAiNavHost(
    startDestination: String,
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    accentColor: AccentColor,
    onSelectAccent: (AccentColor) -> Unit,
    accentStyle: AccentStyle,
    onSelectAccentStyle: (AccentStyle) -> Unit,
    reduceMotion: Boolean,
    onSetReduceMotion: (Boolean) -> Unit,
    highContrast: Boolean,
    onSetHighContrast: (Boolean) -> Unit,
    haptics: Boolean,
    onSetHaptics: (Boolean) -> Unit,
    autoLockScreen: Boolean,
    onSetAutoLockScreen: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            BeamHubScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAuth = { navController.navigate(Routes.AUTH) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onSelectTheme = onSelectTheme,
                accentColor = accentColor,
                onSelectAccent = onSelectAccent,
                accentStyle = accentStyle,
                onSelectAccentStyle = onSelectAccentStyle,
                reduceMotion = reduceMotion,
                onSetReduceMotion = onSetReduceMotion,
                highContrast = highContrast,
                onSetHighContrast = onSetHighContrast,
                haptics = haptics,
                onSetHaptics = onSetHaptics,
                autoLockScreen = autoLockScreen,
                onSetAutoLockScreen = onSetAutoLockScreen,
                onOpenAuth = { navController.navigate(Routes.AUTH) },
                onOpenGod = { if (BuildConfig.DEBUG) navController.navigate(Routes.GOD) },
                onOpenSoundAlert = { navController.navigate(Routes.SOUND_ALERT) },
                onOpenLedBanner = { navController.navigate(Routes.LED_BANNER) },
                onOpenPrivacyPolicy = { navController.navigate(Routes.LEGAL_PRIVACY) },
                onOpenTerms = { navController.navigate(Routes.LEGAL_TERMS) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AUTH) {
            AuthScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        // God existe SOLO en debug: en release la ruta no se registra, así que ni siquiera
        // es navegable (ni por deep link ni por un navigate() olvidado en el código).
        if (BuildConfig.DEBUG) {
            composable(Routes.GOD) {
                GodScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.SOUND_ALERT) {
            SoundAlertScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LED_BANNER) {
            LedBannerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEGAL_PRIVACY) {
            LegalWebScreen(
                title = stringResource(com.mejoresiagratis.lumiai.R.string.legal_privacy),
                url = LegalUrls.PRIVACY,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LEGAL_TERMS) {
            LegalWebScreen(
                title = stringResource(com.mejoresiagratis.lumiai.R.string.legal_terms),
                url = LegalUrls.TERMS,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
