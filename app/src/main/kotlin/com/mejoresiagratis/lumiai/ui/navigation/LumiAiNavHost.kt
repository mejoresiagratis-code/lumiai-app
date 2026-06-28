package com.mejoresiagratis.lumiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.auth.AuthScreen
import com.mejoresiagratis.lumiai.ui.god.GodScreen
import com.mejoresiagratis.lumiai.ui.home.beamhub.BeamHubScreen
import com.mejoresiagratis.lumiai.ui.onboarding.OnboardingScreen
import com.mejoresiagratis.lumiai.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val AUTH = "auth"
    const val GOD = "god"
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
                onOpenGod = { navController.navigate(Routes.GOD) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AUTH) {
            AuthScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.GOD) {
            GodScreen(onBack = { navController.popBackStack() })
        }
    }
}
