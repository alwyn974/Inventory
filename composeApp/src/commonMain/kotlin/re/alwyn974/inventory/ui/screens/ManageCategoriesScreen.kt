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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.CategoryDto
import re.alwyn974.inventory.shared.model.CreateCategoryRequest
import re.alwyn974.inventory.shared.model.UpdateCategoryRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryDto?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadCategories(apiClient) { result ->
            when (result) {
                is Result.Success -> {
                    categories = result.data
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
                title = { Text("Gérer les catégories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une catégorie")
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
                                    loadCategories(apiClient) { result ->
                                        when (result) {
                                            is Result.Success -> {
                                                categories = result.data
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
                categories.isEmpty() -> {
                    Text(
                        text = "Aucune catégorie",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryCard(
                                category = category,
                                onEdit = { editingCategory = category },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteCategory(category.id)
                                            loadCategories(apiClient) { result ->
                                                if (result is Result.Success) {
                                                    categories = result.data
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
        CreateEditCategoryDialog(
            apiClient = apiClient,
            category = null,
            onDismiss = { showCreateDialog = false },
            onSaved = {
                showCreateDialog = false
                scope.launch {
                    loadCategories(apiClient) { result ->
                        if (result is Result.Success) {
                            categories = result.data
                        }
                    }
                }
            }
        )
    }

    editingCategory?.let { category ->
        CreateEditCategoryDialog(
            apiClient = apiClient,
            category = category,
            onDismiss = { editingCategory = null },
            onSaved = {
                editingCategory = null
                scope.launch {
                    loadCategories(apiClient) { result ->
                        if (result is Result.Success) {
                            categories = result.data
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: CategoryDto,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                category.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
            title = { Text("Supprimer la catégorie") },
            text = { Text("Êtes-vous sûr de vouloir supprimer \"${category.name}\" ?") },
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
fun CreateEditCategoryDialog(
    apiClient: ApiClient,
    category: CategoryDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Créer une catégorie" else "Modifier la catégorie") },
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
                    if (name.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (category == null) {
                                    apiClient.createCategory(
                                        CreateCategoryRequest(
                                            name = name,
                                            description = description.ifBlank { null }
                                        )
                                    )
                                } else {
                                    apiClient.updateCategory(
                                        category.id,
                                        UpdateCategoryRequest(
                                            name = name,
                                            description = description.ifBlank { null }
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

suspend fun loadCategories(
    apiClient: ApiClient,
    onResult: (Result<List<CategoryDto>>) -> Unit
) {
    try {
        val categories = apiClient.getCategories()
        onResult(Result.Success(categories))
    } catch (e: Exception) {
        onResult(Result.Error(e.message ?: "Erreur inconnue"))
    }
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
