package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.ItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    apiClient: ApiClient,
    onLogout: () -> Unit,
    onCreateItem: () -> Unit,
    onEditItem: (ItemDto) -> Unit,
    onManageCategories: () -> Unit,
    onManageTags: () -> Unit,
    onManageFolders: () -> Unit
) {
    var items by remember { mutableStateOf<List<ItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadItems(apiClient,
            onSuccess = { itemsList ->
                items = itemsList
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventaire") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Gérer les catégories") },
                            onClick = {
                                showMenu = false
                                onManageCategories()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gérer les tags") },
                            onClick = {
                                showMenu = false
                                onManageTags()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gérer les dossiers") },
                            onClick = {
                                showMenu = false
                                onManageFolders()
                            }
                        )
                        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                        DropdownMenuItem(
                            text = { Text("Déconnexion") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateItem
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un item")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Erreur: $errorMessage",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    loadItems(apiClient,
                                        onSuccess = { itemsList ->
                                            items = itemsList
                                            isLoading = false
                                        },
                                        onError = { error ->
                                            errorMessage = error
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
                items.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aucun item trouvé",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateItem) {
                            Text("Créer votre premier item")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            ItemCard(
                                item = item,
                                onEdit = { onEditItem(item) },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteItem(item.id)
                                            loadItems(apiClient,
                                                onSuccess = { itemsList ->
                                                    items = itemsList
                                                },
                                                onError = { error ->
                                                    errorMessage = error
                                                }
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = "Erreur lors de la suppression: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: ItemDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    item.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Text(
                        text = "Quantité: ${item.quantity}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    item.category?.let { category ->
                        Text(
                            text = "Catégorie: ${category.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    item.folder?.let { folder ->
                        Text(
                            text = "Dossier: ${folder.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (item.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.tags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag.name) }
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'item") },
            text = { Text("Êtes-vous sûr de vouloir supprimer \"${item.name}\" ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

suspend fun loadItems(
    apiClient: ApiClient,
    onSuccess: (List<ItemDto>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val items = apiClient.getItems()
        onSuccess(items)
    } catch (e: Exception) {
        onError(e.message ?: "Erreur inconnue")
    }
}
