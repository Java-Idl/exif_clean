package com.example.exifclean.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.exifclean.data.IntakeItem
import com.example.exifclean.data.SanitizationResult

sealed class Screen(val route: String, val title: String) {
    object Intake : Screen("intake", "Intake")
    object Vault : Screen("vault", "Vault")
    object Settings : Screen("settings", "Settings")
    object Onboarding : Screen("onboarding", "Guarantee Model")
    object Detail : Screen("detail", "Metadata Detail")
    object Diff : Screen("diff", "Sanitization Diff")
}

@Composable
fun AppNavigation(
    intakeState: IntakeUiState,
    vaultItems: List<com.example.exifclean.core.vault.VaultItem>,
    settings: AppSettings,
    onUrisPicked: (List<android.net.Uri>) -> Unit,
    onRemoveIntakeItem: (IntakeItem) -> Unit,
    onClearIntakeCache: () -> Unit,
    onInspectMetadata: (IntakeItem) -> com.example.exifclean.core.exif.MetadataReport,
    onSanitizeItem: (IntakeItem) -> SanitizationResult,
    onImportToVault: () -> Unit,
    onExportVaultItem: (com.example.exifclean.core.vault.VaultItem) -> Unit,
    onShredVaultItem: (com.example.exifclean.core.vault.VaultItem) -> Unit,
    onShredEntireVault: () -> Unit,
    onSettingsChanged: (AppSettings) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedIntakeItem by remember { mutableStateOf<IntakeItem?>(null) }
    var currentDiffResult by remember { mutableStateOf<SanitizationResult?>(null) }

    Scaffold(
        bottomBar = {
            if (currentRoute == Screen.Intake.route || currentRoute == Screen.Vault.route || currentRoute == Screen.Settings.route) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Intake") },
                        label = { Text("Intake") },
                        selected = currentRoute == Screen.Intake.route,
                        onClick = {
                            navController.navigate(Screen.Intake.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                        label = { Text("Vault") },
                        selected = currentRoute == Screen.Vault.route,
                        onClick = {
                            navController.navigate(Screen.Vault.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = Screen.Intake.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Intake.route) {
                IntakeScreen(
                    state = intakeState,
                    onUrisPicked = onUrisPicked,
                    onRemoveItem = onRemoveIntakeItem,
                    onClearAll = onClearIntakeCache,
                    onInspectItem = { item ->
                        selectedIntakeItem = item
                        val res = onSanitizeItem(item)
                        currentDiffResult = res
                        navController.navigate(Screen.Diff.route)
                    }
                )
            }

            composable(Screen.Vault.route) {
                VaultScreen(
                    items = vaultItems,
                    onExportItem = onExportVaultItem,
                    onShredItem = onShredVaultItem,
                    onShredAll = onShredEntireVault,
                    onImportToVault = onImportToVault
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChanged = onSettingsChanged,
                    onClearAllCaches = onClearIntakeCache,
                    onOpenOnboarding = { navController.navigate(Screen.Onboarding.route) }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.Diff.route) {
                currentDiffResult?.let { res ->
                    SanitizationDiffScreen(
                        result = res,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
