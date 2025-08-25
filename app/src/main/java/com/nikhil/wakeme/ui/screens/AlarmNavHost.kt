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
import com.nikhil.wakeme.viewmodels.AlarmListViewModel

@Composable
fun AlarmNavHost(
    alarmListViewModel: AlarmListViewModel,
    requestPermission: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            AlarmListScreen(
                onItemClick = { id ->
                    val alarms = alarmListViewModel.uiState.value
                    val selectedAlarm =
                        if (alarms is com.nikhil.wakeme.util.Resource.Success) alarms.data.find { it.id == id } else null
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "alarm",
                        selectedAlarm
                    )
                    navController.navigate(Routes.edit(id))
                },
                viewModel = alarmListViewModel,
                requestPermission = requestPermission
            )
        }

        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            val navAlarm = navController.previousBackStackEntry
                ?.savedStateHandle?.get<com.nikhil.wakeme.data.Alarm>("alarm")

            AlarmEditScreen(
                alarmId = alarmId,
                isNewAlarm = alarmId == 0L,
                goBack = { navController.popBackStack() },
                loadedAlarm = navAlarm,
                viewModel = viewModel()
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
