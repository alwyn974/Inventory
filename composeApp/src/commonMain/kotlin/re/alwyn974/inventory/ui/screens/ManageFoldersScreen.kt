package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.models.*
import re.alwyn974.inventory.network.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFoldersScreen(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var folders by remember { mutableStateOf<List<FolderDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderDto?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadFolders(apiClient) { result ->
            when (result) {
                is Result.Success -> {
                    folders = result.data
                    isLoading = false
                }
                is Result.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gérer les dossiers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un dossier")
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
                                    loadFolders(apiClient) { result ->
                                        when (result) {
                                            is Result.Success -> {
                                                folders = result.data
                                                isLoading = false
                                            }
                                            is Result.Error -> {
                                                errorMessage = result.message
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
                folders.isEmpty() -> {
                    Text(
                        text = "Aucun dossier",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(folders) { folder ->
                            FolderCard(
                                folder = folder,
                                onEdit = { editingFolder = folder },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteFolder(folder.id)
                                            loadFolders(apiClient) { result ->
                                                if (result is Result.Success) {
                                                    folders = result.data
                                                }
                                            }
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

    if (showCreateDialog) {
        CreateEditFolderDialog(
            apiClient = apiClient,
            folder = null,
            availableFolders = folders,
            onDismiss = { showCreateDialog = false },
            onSaved = {
                showCreateDialog = false
                scope.launch {
                    loadFolders(apiClient) { result ->
                        if (result is Result.Success) {
                            folders = result.data
                        }
                    }
                }
            }
        )
    }

    editingFolder?.let { folder ->
        CreateEditFolderDialog(
            apiClient = apiClient,
            folder = folder,
            availableFolders = folders.filter { it.id != folder.id },
            onDismiss = { editingFolder = null },
            onSaved = {
                editingFolder = null
                scope.launch {
                    loadFolders(apiClient) { result ->
                        if (result is Result.Success) {
                            folders = result.data
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun FolderCard(
    folder: FolderDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Dossier",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = folder.fullPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    folder.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le dossier") },
            text = { Text("Êtes-vous sûr de vouloir supprimer \"${folder.name}\" ?") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditFolderDialog(
    apiClient: ApiClient,
    folder: FolderDto?,
    availableFolders: List<FolderDto>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var description by remember { mutableStateOf(folder?.description ?: "") }
    var fullPath by remember { mutableStateOf(folder?.fullPath ?: "") }
    var selectedParent by remember { mutableStateOf(availableFolders.find { it.id == folder?.parentFolderId }) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showParentDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Créer un dossier" else "Modifier le dossier") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom *") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = name.isBlank()
                )

                OutlinedTextField(
                    value = fullPath,
                    onValueChange = { fullPath = it },
                    label = { Text("Chemin complet *") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = fullPath.isBlank()
                )

                ExposedDropdownMenuBox(
                    expanded = showParentDropdown,
                    onExpandedChange = { showParentDropdown = !showParentDropdown && !isLoading }
                ) {
                    OutlinedTextField(
                        value = selectedParent?.name ?: "Aucun dossier parent",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Dossier parent") },
                        modifier = Modifier.menuAnchor(),
                        enabled = !isLoading,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showParentDropdown)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showParentDropdown,
                        onDismissRequest = { showParentDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucun dossier parent") },
                            onClick = {
                                selectedParent = null
                                showParentDropdown = false
                            }
                        )
                        availableFolders.forEach { parentFolder ->
                            DropdownMenuItem(
                                text = { Text("${parentFolder.name} (${parentFolder.fullPath})") },
                                onClick = {
                                    selectedParent = parentFolder
                                    showParentDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && fullPath.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (folder == null) {
                                    apiClient.createFolder(
                                        CreateFolderRequest(
                                            name = name,
                                            description = description.ifBlank { null },
                                            fullPath = fullPath,
                                            parentFolderId = selectedParent?.id
                                        )
                                    )
                                } else {
                                    apiClient.updateFolder(
                                        folder.id,
                                        UpdateFolderRequest(
                                            name = name,
                                            description = description.ifBlank { null },
                                            fullPath = fullPath,
                                            parentFolderId = selectedParent?.id
                                        )
                                    )
                                }
                                onSaved()
                            } catch (e: Exception) {
                                errorMessage = "Erreur: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && fullPath.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enregistrer")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler")
            }
        }
    )
}

suspend fun loadFolders(
    apiClient: ApiClient,
    onResult: (Result<List<FolderDto>>) -> Unit
) {
    try {
        val folders = apiClient.getFolders()
        onResult(Result.Success(folders))
    } catch (e: Exception) {
        onResult(Result.Error(e.message ?: "Erreur inconnue"))
    }
}
