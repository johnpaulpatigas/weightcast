package com.johnpaulpatigas.weightcast.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.johnpaulpatigas.weightcast.feature.dashboard.DashboardScreen
import com.johnpaulpatigas.weightcast.feature.dashboard.DashboardViewModel
import com.johnpaulpatigas.weightcast.feature.goals_settings.GoalsSettingsScreen
import com.johnpaulpatigas.weightcast.feature.goals_settings.GoalsSettingsViewModel
import com.johnpaulpatigas.weightcast.feature.weight_history.WeightHistoryScreen
import com.johnpaulpatigas.weightcast.feature.weight_history.WeightHistoryViewModel
import com.johnpaulpatigas.weightcast.feature.weight_logging.LogWeightBottomSheet
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    loggingViewModel: WeightLoggingViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    historyViewModel: WeightHistoryViewModel = hiltViewModel(),
    goalsSettingsViewModel: GoalsSettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val loggingUiState by loggingViewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isExpandedWidth = configuration.screenWidthDp >= 600

    fun navigateToAnalytics() {
        navController.navigate(Screen.Analytics.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isExpandedWidth) {
        // Adaptive Navigation Rail for larger screens / tablets
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onQuickAddClick = { loggingViewModel.onOpenAddForm() },
                            onNavigateToAnalytics = { navigateToAnalytics() }
                        )
                    }
                    composable(Screen.Analytics.route) {
                        WeightHistoryScreen(
                            viewModel = historyViewModel,
                            onEditEntryClick = { entry -> loggingViewModel.onOpenEditForm(entry) }
                        )
                    }
                    composable(Screen.Goals.route) {
                        GoalsSettingsScreen(viewModel = goalsSettingsViewModel)
                    }
                }
            }
        }
    } else {
        // Standard NavigationBar for Compact Screens
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onQuickAddClick = { loggingViewModel.onOpenAddForm() },
                        onNavigateToAnalytics = { navigateToAnalytics() }
                    )
                }
                composable(Screen.Analytics.route) {
                    WeightHistoryScreen(
                        viewModel = historyViewModel,
                        onEditEntryClick = { entry -> loggingViewModel.onOpenEditForm(entry) }
                    )
                }
                composable(Screen.Goals.route) {
                    GoalsSettingsScreen(viewModel = goalsSettingsViewModel)
                }
            }
        }
    }

    if (loggingUiState.isFormOpen) {
        LogWeightBottomSheet(
            uiState = loggingUiState,
            onWeightInputChanged = { loggingViewModel.onWeightInputChanged(it) },
            onTimestampSelected = { loggingViewModel.onTimestampSelected(it) },
            onNotesInputChanged = { loggingViewModel.onNotesInputChanged(it) },
            onSaveEntry = { allowDup -> loggingViewModel.onSaveEntry(allowDup) },
            onDismissRequest = { loggingViewModel.onCloseForm() }
        )
    }
}
