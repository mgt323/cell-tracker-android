package com.mgt323.celltracker.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main_screen")
    object Detail : Screen("detail_screen")
    object CurrentConnection : Screen("current_connection_screen")
}