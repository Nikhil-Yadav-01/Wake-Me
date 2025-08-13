package com.nikhil.wakeme.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AlarmNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "list") {
        composable("list") { AlarmListScreen(nav) }
        composable("edit") { AlarmEditScreen(nav) }
    }
}
