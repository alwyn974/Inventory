package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.CreateTagRequest
import re.alwyn974.inventory.shared.model.TagDto
import re.alwyn974.inventory.ui.utils.ColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsScreen(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var tags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<TagDto?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadTags(apiClient) { result ->
            when (result) {
                is Result.Success -> {
                    tags = result.data
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
                title = { Text("Manage Tags") },
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
                Icon(Icons.Default.Add, contentDescription = "Add tag")
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
                                    loadTags(apiClient) { result ->
                                        when (result) {
                                            is Result.Success -> {
                                                tags = result.data
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
                tags.isEmpty() -> {
                    Text(
                        text = "No tags",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            TagCard(
                                tag = tag,
                                onEdit = { editingTag = tag },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteTag(tag.id)
                                            loadTags(apiClient) { result ->
                                                if (result is Result.Success) {
                                                    tags = result.data
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Error deleting tag: ${e.message}"
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
        CreateEditTagDialog(
            apiClient = apiClient,
            tag = null,
            onDismiss = { showCreateDialog = false },
            onSaved = {
                showCreateDialog = false
                scope.launch {
                    loadTags(apiClient) { result ->
                        if (result is Result.Success) {
                            tags = result.data
                        }
                    }
                }
            }
        )
    }

    editingTag?.let { tag ->
        CreateEditTagDialog(
            apiClient = apiClient,
            tag = tag,
            onDismiss = { editingTag = null },
            onSaved = {
                editingTag = null
                scope.launch {
                    loadTags(apiClient) { result ->
                        if (result is Result.Success) {
                            tags = result.data
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun TagCard(
    tag: TagDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val color = try {
        ColorUtils.parseColor(tag.color ?: "#808080")
    } catch (e: Exception) {
        Color.Gray
    }

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
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
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
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete \"${tag.name}\"?") },
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
fun CreateEditTagDialog(
    apiClient: ApiClient,
    tag: TagDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var color by remember { mutableStateOf(tag?.color ?: "#FF0000") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tag == null) "Create Tag" else "Edit Tag") },
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
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (hex)") },
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("#FF0000") }
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
                    if (name.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (tag == null) {
                                    apiClient.createTag(
                                        CreateTagRequest(
                                            name = name,
                                            color = color.ifBlank { null }
                                        )
                                    )
                                } else {
                                    apiClient.updateTag(
                                        tag.id,
                                        CreateTagRequest(
                                            name = name,
                                            color = color.ifBlank { null }
                                        )
                                    )
                                }
                                onSaved()
                            } catch (e: Exception) {
                                errorMessage = "Error saving tag: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Name is required"
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

suspend fun loadTags(
    apiClient: ApiClient,
    onResult: (Result<List<TagDto>>) -> Unit
) {
    try {
        val tags = apiClient.getTags()
        onResult(Result.Success(tags))
    } catch (e: Exception) {
        onResult(Result.Error(e.message ?: "Unknown error"))
    }
}
