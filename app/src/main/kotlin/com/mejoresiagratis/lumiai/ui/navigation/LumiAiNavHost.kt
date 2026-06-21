package com.mejoresiagratis.lumiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.ui.home.HomeScreen
import com.mejoresiagratis.lumiai.ui.onboarding.OnboardingScreen
import com.mejoresiagratis.lumiai.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun LumiAiNavHost(
    startDestination: String,
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit
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
            HomeScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onSelectTheme = onSelectTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
