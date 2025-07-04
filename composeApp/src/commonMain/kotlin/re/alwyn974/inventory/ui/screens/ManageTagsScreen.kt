package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import re.alwyn974.inventory.models.*
import re.alwyn974.inventory.network.ApiClient
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
                title = { Text("Gérer les tags") },
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
                Icon(Icons.Default.Add, contentDescription = "Ajouter un tag")
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
                            Text("Réessayer")
                        }
                    }
                }
                tags.isEmpty() -> {
                    Text(
                        text = "Aucun tag",
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
                tag.color?.let { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = ColorUtils.parseColor(color),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
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
            title = { Text("Supprimer le tag") },
            text = { Text("Êtes-vous sûr de vouloir supprimer \"${tag.name}\" ?") },
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

@Composable
fun CreateEditTagDialog(
    apiClient: ApiClient,
    tag: TagDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var color by remember { mutableStateOf(tag?.color ?: "#007AFF") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val colorPresets = listOf(
        "#007AFF", "#FF3B30", "#34C759", "#FF9500", "#AF52DE",
        "#FF2D92", "#00C7BE", "#5AC8FA", "#FFCC00", "#FF6B6B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tag == null) "Créer un tag" else "Modifier le tag") },
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

                Text(
                    text = "Couleur",
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyColumn(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorPresets.chunked(5)) { colorRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorRow.forEach { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = ColorUtils.parseColor(colorHex),
                                            shape = CircleShape
                                        )
                                        .let {
                                            if (color == colorHex) {
                                                it.background(
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = CircleShape
                                                )
                                            } else it
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .background(
                                                color = ColorUtils.parseColor(colorHex),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

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
                                            color = color
                                        )
                                    )
                                } else {
                                    apiClient.updateTag(
                                        tag.id,
                                        CreateTagRequest(
                                            name = name,
                                            color = color
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
                enabled = !isLoading && name.isNotBlank()
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

suspend fun loadTags(
    apiClient: ApiClient,
    onResult: (Result<List<TagDto>>) -> Unit
) {
    try {
        val tags = apiClient.getTags()
        onResult(Result.Success(tags))
    } catch (e: Exception) {
        onResult(Result.Error(e.message ?: "Erreur inconnue"))
    }
}
