package com.tyua.pivottranslator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tyua.pivottranslator.ui.screen.TranslationScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "translation"
    ) {
        composable("translation") {
            TranslationScreen()
        }
    }
}
