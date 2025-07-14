package re.alwyn974.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.ItemDto
import re.alwyn974.inventory.storage.SessionManager
import re.alwyn974.inventory.ui.components.MenuBar
import re.alwyn974.inventory.ui.screens.*

@Composable
fun App() {
    MaterialTheme {
        InventoryApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp() {
    val apiClient = remember { ApiClient() }
    val sessionManager = remember { SessionManager.getInstance() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemDto?>(null) }
    var isCheckingSession by remember { mutableStateOf(true) }

    // Check for existing session on app start
    LaunchedEffect(Unit) {
        sessionManager.loadSavedSession()
        if (sessionManager.hasValidSession()) {
            println("App: Found saved session, redirecting to Items screen")
            isLoggedIn = true
            currentScreen = Screen.Items
            // Initialize ApiClient with saved tokens
            sessionManager.initializeApiClient(apiClient)
        } else {
            println("App: No saved session found, staying on Login screen")
        }
        isCheckingSession = false
    }

    // Use Scaffold to ensure proper layout with topbar
    Scaffold { paddingValues ->
        if (isCheckingSession) {
            // Show loading screen while checking session
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show menubar only when logged in and not on login screen
                if (isLoggedIn && currentScreen != Screen.Login) {
                    MenuBar(
                        onManageCategories = { currentScreen = Screen.ManageCategories },
                        onManageTags = { currentScreen = Screen.ManageTags },
                        onManageFolders = { currentScreen = Screen.ManageFolders },
                        onLogout = {
                            isLoggedIn = false
                            currentScreen = Screen.Login
                            // Clear session when logging out
                            GlobalScope.launch {
                                sessionManager.clearSession()
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Screen content
                when (currentScreen) {
                    Screen.Login -> {
                        LoginScreen(
                            apiClient = apiClient, onLoginSuccess = { token ->
                                isLoggedIn = true
                                currentScreen = Screen.Items
                            })
                    }

                    Screen.Items -> {
                        ItemsScreen(
                            apiClient = apiClient,
                            onCreateItem = {
                                editingItem = null
                                currentScreen = Screen.CreateEditItem
                            },
                            onEditItem = { item ->
                                editingItem = item
                                currentScreen = Screen.CreateEditItem
                            }
                        )
                    }

                    Screen.CreateEditItem -> {
                        CreateEditItemScreen(apiClient = apiClient, item = editingItem, onBack = {
                            currentScreen = Screen.Items
                        }, onSaved = {
                            currentScreen = Screen.Items
                        })
                    }

                    Screen.ManageCategories -> {
                        ManageCategoriesScreen(
                            apiClient = apiClient, onBack = {
                                currentScreen = Screen.Items
                            })
                    }

                    Screen.ManageTags -> {
                        ManageTagsScreen(
                            apiClient = apiClient, onBack = {
                                currentScreen = Screen.Items
                            })
                    }

                    Screen.ManageFolders -> {
                        ManageFoldersScreen(
                            apiClient = apiClient, onBack = {
                                currentScreen = Screen.Items
                            })
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Login : Screen()
    object Items : Screen()
    object CreateEditItem : Screen()
    object ManageCategories : Screen()
    object ManageTags : Screen()
    object ManageFolders : Screen()
}

expect fun getPlatform(): Platform

interface Platform {
    val name: String
}