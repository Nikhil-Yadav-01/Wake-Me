package com.nikhil.wakeme.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel

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
            val viewModel: AlarmEditViewModel = viewModel()
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            val isNewAlarm = alarmId == 0L

            LaunchedEffect(Unit) {
                viewModel.loadAlarm(alarmId, isNewAlarm)
            }

            AlarmEditScreen(
                alarmId = alarmId,
                isNewAlarm = isNewAlarm,
                goBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}

// Routes
data object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{alarmId}"
    fun edit(alarmId: Long) = "edit/$alarmId"
}
