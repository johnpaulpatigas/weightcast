package com.johnpaulpatigas.weightcast.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.johnpaulpatigas.weightcast.feature.dashboard.DashboardScreen
import com.johnpaulpatigas.weightcast.feature.dashboard.DashboardViewModel
import com.johnpaulpatigas.weightcast.feature.weight_history.WeightHistoryScreen
import com.johnpaulpatigas.weightcast.feature.weight_history.WeightHistoryViewModel
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingDialog
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingScreen
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingViewModel

@Composable
fun MainAppScreen(
    loggingViewModel: WeightLoggingViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    historyViewModel: WeightHistoryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val loggingUiState by loggingViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onQuickAddClick = { loggingViewModel.onOpenAddForm() }
                )
            }
            composable(Screen.History.route) {
                WeightHistoryScreen(
                    viewModel = historyViewModel,
                    onEditEntryClick = { entry -> loggingViewModel.onOpenEditForm(entry) }
                )
            }
            composable(Screen.Logs.route) {
                WeightLoggingScreen(
                    viewModel = loggingViewModel
                )
            }
        }
    }

    if (loggingUiState.isFormOpen) {
        WeightLoggingDialog(
            uiState = loggingUiState,
            onWeightInputChanged = { loggingViewModel.onWeightInputChanged(it) },
            onUnitSelected = { loggingViewModel.onUnitSelected(it) },
            onTimestampSelected = { loggingViewModel.onTimestampSelected(it) },
            onNotesInputChanged = { loggingViewModel.onNotesInputChanged(it) },
            onSaveEntry = { allowDup -> loggingViewModel.onSaveEntry(allowDup) },
            onDismissRequest = { loggingViewModel.onCloseForm() }
        )
    }
}
