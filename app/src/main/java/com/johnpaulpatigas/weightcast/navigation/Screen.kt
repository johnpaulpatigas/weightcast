package com.johnpaulpatigas.weightcast.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)

    companion object {
        val bottomNavItems = listOf(Dashboard, History, Logs)
    }
}
