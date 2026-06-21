package com.mejoresiagratis.lumiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mejoresiagratis.lumiai.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
}

@Composable
fun LumiAiNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen() }
    }
}
