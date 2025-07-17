package re.alwyn974.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.ItemDto
import re.alwyn974.inventory.storage.SessionManager
import re.alwyn974.inventory.ui.components.NavigationDrawer
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    if (isCheckingSession) {
        // Show loading screen while checking session
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (!isLoggedIn || currentScreen == Screen.Login) {
        // Show login screen without drawer
        LoginScreen(
            apiClient = apiClient,
            onLoginSuccess = { token ->
                isLoggedIn = true
                currentScreen = Screen.Items
            }
        )
    } else {
        // Show main app with navigation drawer
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawer(
                    currentScreen = currentScreen.toString(),
                    onNavigateToItems = {
                        currentScreen = Screen.Items
                        scope.launch { drawerState.close() }
                    },
                    onManageCategories = {
                        currentScreen = Screen.ManageCategories
                        scope.launch { drawerState.close() }
                    },
                    onManageTags = {
                        currentScreen = Screen.ManageTags
                        scope.launch { drawerState.close() }
                    },
                    onManageFolders = {
                        currentScreen = Screen.ManageFolders
                        scope.launch { drawerState.close() }
                    },
                    onLogout = {
                        isLoggedIn = false
                        currentScreen = Screen.Login
                        scope.launch {
                            drawerState.close()
                            sessionManager.clearSession()
                        }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(when (currentScreen) {
                                Screen.Items -> "Inventory"
                                Screen.CreateEditItem -> if (editingItem != null) "Edit Item" else "Create Item"
                                Screen.ManageCategories -> "Manage Categories"
                                Screen.ManageTags -> "Manage Tags"
                                Screen.ManageFolders -> "Manage Folders"
                                else -> "Inventory"
                            })
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) {
                                            drawerState.open()
                                        } else {
                                            drawerState.close()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
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
                            CreateEditItemScreen(
                                apiClient = apiClient,
                                item = editingItem,
                                onBack = {
                                    currentScreen = Screen.Items
                                },
                                onSaved = {
                                    currentScreen = Screen.Items
                                }
                            )
                        }

                        Screen.ManageCategories -> {
                            ManageCategoriesScreen(
                                apiClient = apiClient,
                                onBack = {
                                    currentScreen = Screen.Items
                                }
                            )
                        }

                        Screen.ManageTags -> {
                            ManageTagsScreen(
                                apiClient = apiClient,
                                onBack = {
                                    currentScreen = Screen.Items
                                }
                            )
                        }

                        Screen.ManageFolders -> {
                            ManageFoldersScreen(
                                apiClient = apiClient,
                                onBack = {
                                    currentScreen = Screen.Items
                                }
                            )
                        }

                        else -> {
                            // Fallback
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