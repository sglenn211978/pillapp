package com.sglenn.dosepulse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sglenn.dosepulse.ui.dashboard.DashboardScreen
import com.sglenn.dosepulse.ui.database.MedicationDatabaseScreen
import com.sglenn.dosepulse.ui.edit.AddEditMedicationScreen
import com.sglenn.dosepulse.ui.history.HistoryScreen
import com.sglenn.dosepulse.ui.theme.DosePulseTheme
import com.sglenn.dosepulse.ui.theme.ThemeManager
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModel
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModelFactory
import com.sglenn.dosepulse.alarm.MedicationAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionRefresh = MutableStateFlow(0)
    private var alarmsRebuiltThisProcess = false

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory((application as PillApplication).repository)
    }

    private val requestNotificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val permissionRefreshKey by permissionRefresh.collectAsState()
            val themeState by ThemeManager.themeState.collectAsState()

            DosePulseTheme(themeState = themeState) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        // Only show bottom navigation on top-level tabs
                        if (currentRoute in listOf("dashboard", "database", "history")) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "dashboard",
                                    onClick = {
                                        navController.navigate("dashboard") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    label = { Text("Today") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "database",
                                    onClick = {
                                        navController.navigate("database") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                    label = { Text("Drug Guide") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "history",
                                    onClick = {
                                        navController.navigate("history") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text("History") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                permissionRefreshKey = permissionRefreshKey,
                                onNavigateToAddEdit = { medId ->
                                    navController.navigate("add_edit/$medId")
                                }
                            )
                        }

                        composable("database") {
                            MedicationDatabaseScreen(
                                viewModel = viewModel,
                                onMedicationSelected = { name ->
                                    navController.navigate("add_from_guide/${Uri.encode(name)}")
                                }
                            )
                        }

                        composable("history") {
                            HistoryScreen(viewModel = viewModel)
                        }

                        composable(
                            route = "add_edit/{medicationId}",
                            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val medId = backStackEntry.arguments?.getLong("medicationId") ?: -1L
                            AddEditMedicationScreen(
                                medicationId = medId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "add_from_guide/{medicationName}",
                            arguments = listOf(navArgument("medicationName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val medicationName = backStackEntry.arguments?.getString("medicationName").orEmpty()
                            AddEditMedicationScreen(
                                medicationId = -1L,
                                viewModel = viewModel,
                                initialName = medicationName,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh.value += 1
        if (!alarmsRebuiltThisProcess && MedicationAlarmScheduler.canScheduleExactAlarms(this)) {
            alarmsRebuiltThisProcess = true
            lifecycleScope.launch(Dispatchers.IO) {
                (application as PillApplication).repository.allMedications.first()
                    .filter { it.isActive }
                    .forEach { MedicationAlarmScheduler.scheduleNextAlarm(this@MainActivity, it) }
            }
        }
    }
}
