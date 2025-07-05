package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.CreateFolderRequest
import re.alwyn974.inventory.shared.model.FolderDto
import re.alwyn974.inventory.shared.model.UpdateFolderRequest

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
                title = { Text("Manage Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add folder")
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
                            text = "Error: $errorMessage",
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
                            Text("Retry")
                        }
                    }
                }
                folders.isEmpty() -> {
                    Text(
                        text = "No folders",
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
                                            errorMessage = "Error deleting folder: ${e.message}"
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Folder",
                    modifier = Modifier.size(24.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    folder.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete \"${folder.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreateEditFolderDialog(
    apiClient: ApiClient,
    folder: FolderDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var fullPath by remember { mutableStateOf(folder?.fullPath ?: "") }
    var description by remember { mutableStateOf(folder?.description ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Create Folder" else "Edit Folder") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = name.isBlank()
                )

                OutlinedTextField(
                    value = fullPath,
                    onValueChange = { fullPath = it },
                    label = { Text("Full Path *") },
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("/storage/folder-name") },
                    isError = fullPath.isBlank()
                )

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
                                            fullPath = fullPath,
                                            description = description.ifBlank { null }
                                        )
                                    )
                                } else {
                                    apiClient.updateFolder(
                                        folder.id,
                                        UpdateFolderRequest(
                                            name = name,
                                            fullPath = fullPath,
                                            description = description.ifBlank { null }
                                        )
                                    )
                                }
                                onSaved()
                            } catch (e: Exception) {
                                errorMessage = "Error saving folder: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = when {
                            name.isBlank() -> "Name is required"
                            fullPath.isBlank() -> "Full path is required"
                            else -> "Please fill in all required fields"
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        onResult(Result.Error(e.message ?: "Unknown error"))
    }
}
