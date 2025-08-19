package com.nikhil.wakeme.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AlarmNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            AlarmListScreen(
                onItemClick = { id ->
                    navController.navigate(Routes.edit(id))
                }
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            AlarmEditScreen(goBack = { navController.popBackStack() }, alarmId = alarmId)
        }
    }
}

// Routes
data object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{alarmId}"
    fun edit(alarmId: Long) = "edit/$alarmId"
}
