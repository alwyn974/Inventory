package re.alwyn974.inventory

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.ItemDto
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemDto?>(null) }

    // Use Scaffold to ensure proper layout with topbar
    Scaffold { paddingValues ->
        when (currentScreen) {
            Screen.Login -> {
                LoginScreen(
                    apiClient = apiClient, onLoginSuccess = { token ->
                        isLoggedIn = true
                        currentScreen = Screen.Items
                    })
            }

            Screen.Items -> {
                ItemsScreen(apiClient = apiClient, onLogout = {
                    isLoggedIn = false
                    currentScreen = Screen.Login
                }, onCreateItem = {
                    editingItem = null
                    currentScreen = Screen.CreateEditItem
                }, onEditItem = { item ->
                    editingItem = item
                    currentScreen = Screen.CreateEditItem
                }, onManageCategories = {
                    currentScreen = Screen.ManageCategories
                }, onManageTags = {
                    currentScreen = Screen.ManageTags
                }, onManageFolders = {
                    currentScreen = Screen.ManageFolders
                })
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