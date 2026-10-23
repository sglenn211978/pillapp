package com.pillease.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pillease.app.ui.dashboard.DashboardScreen
import com.pillease.app.ui.edit.AddEditMedicationScreen
import com.pillease.app.ui.history.HistoryScreen
import com.pillease.app.ui.theme.PillEaseTheme
import com.pillease.app.ui.viewmodel.MedicationViewModel
import com.pillease.app.ui.viewmodel.MedicationViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MedicationViewModel by viewModels {
        MedicationViewModelFactory((application as PillApplication).repository)
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            // Notification permission granted or denied
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PillEaseTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAddEdit = { medId ->
                                navController.navigate("add_edit/$medId")
                            },
                            onNavigateToHistory = {
                                navController.navigate("history")
                            }
                        )
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

                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
