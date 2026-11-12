package com.johnpaulpatigas.weightcast.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun navigateTo(route: String) {
        navController.navigate(route) {
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
                            onClick = { navigateTo(screen.route) }
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
                            onEditEntryClick = { entry -> loggingViewModel.onOpenEditForm(entry) },
                            onDeleteEntryClick = { entry -> loggingViewModel.onDeleteEntry(entry.id) },
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
        // Compact Screens: Floating FAB Navigation Pill
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onQuickAddClick = { loggingViewModel.onOpenAddForm() },
                        onEditEntryClick = { entry -> loggingViewModel.onOpenEditForm(entry) },
                        onDeleteEntryClick = { entry -> loggingViewModel.onDeleteEntry(entry.id) },
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

            val density = LocalDensity.current
            var navPillHeightDp by remember { mutableStateOf(52.dp) }

            // Floating Navigation FAB Pill + Quick Add Squircle FAB
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Floating Navigation Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.onSizeChanged { size ->
                        if (size.height > 0) {
                            with(density) {
                                navPillHeightDp = size.height.toDp()
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(containerColor)
                                    .clickable { navigateTo(screen.route) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                AnimatedVisibility(visible = selected) {
                                    Row {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Floating Log Weight Squircle FAB (same height as navigation pill)
                val squircleCorner = navPillHeightDp * 0.38f
                Surface(
                    onClick = { loggingViewModel.onOpenAddForm() },
                    shape = RoundedCornerShape(squircleCorner),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(navPillHeightDp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Weight",
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
