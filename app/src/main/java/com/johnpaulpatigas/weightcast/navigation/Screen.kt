package com.johnpaulpatigas.weightcast.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    data object Goals : Screen("goals", "Goals & Settings", Icons.Default.Flag)

    companion object {
        val bottomNavItems = listOf(Dashboard, Analytics, Goals)
    }
}
