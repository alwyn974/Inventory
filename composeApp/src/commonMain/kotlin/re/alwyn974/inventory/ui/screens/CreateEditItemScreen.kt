package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import re.alwyn974.inventory.models.*
import re.alwyn974.inventory.network.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditItemScreen(
    apiClient: ApiClient,
    item: ItemDto? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var selectedCategory by remember { mutableStateOf(item?.category) }
    var selectedFolder by remember { mutableStateOf(item?.folder) }
    var selectedTags by remember { mutableStateOf(item?.tags ?: emptyList()) }

    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderDto>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TagDto>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            categories = apiClient.getCategories()
            folders = apiClient.getFolders()
            tags = apiClient.getTags()
        } catch (e: Exception) {
            errorMessage = "Erreur lors du chargement des données: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (item == null) "Créer un item" else "Modifier l'item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    isError = name.isBlank()
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5
                )
            }

            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            quantity = it
                        }
                    },
                    label = { Text("Quantité *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantity.toIntOrNull() == null
                )
            }

            item {
                CategoryDropdown(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    enabled = !isLoading
                )
            }

            item {
                FolderDropdown(
                    folders = folders,
                    selectedFolder = selectedFolder,
                    onFolderSelected = { selectedFolder = it },
                    enabled = !isLoading
                )
            }

            item {
                TagSelector(
                    tags = tags,
                    selectedTags = selectedTags,
                    onTagsChanged = { selectedTags = it },
                    enabled = !isLoading
                )
            }

            item {
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && quantity.toIntOrNull() != null) {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        if (item == null) {
                                            apiClient.createItem(
                                                CreateItemRequest(
                                                    name = name,
                                                    description = description.ifBlank { null },
                                                    quantity = quantity.toInt(),
                                                    categoryId = selectedCategory?.id,
                                                    folderId = selectedFolder?.id,
                                                    tagIds = selectedTags.map { it.id }
                                                )
                                            )
                                        } else {
                                            apiClient.updateItem(
                                                item.id,
                                                UpdateItemRequest(
                                                    name = name,
                                                    description = description.ifBlank { null },
                                                    quantity = quantity.toInt(),
                                                    categoryId = selectedCategory?.id,
                                                    folderId = selectedFolder?.id,
                                                    tagIds = selectedTags.map { it.id }
                                                )
                                            )
                                        }
                                        onSaved()
                                    } catch (e: Exception) {
                                        errorMessage = "Erreur lors de la sauvegarde: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && name.isNotBlank() && quantity.toIntOrNull() != null
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<CategoryDto>,
    selectedCategory: CategoryDto?,
    onCategorySelected: (CategoryDto?) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled }
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Aucune catégorie",
            onValueChange = { },
            readOnly = true,
            label = { Text("Catégorie") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Aucune catégorie") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDropdown(
    folders: List<FolderDto>,
    selectedFolder: FolderDto?,
    onFolderSelected: (FolderDto?) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled }
    ) {
        OutlinedTextField(
            value = selectedFolder?.name ?: "Aucun dossier",
            onValueChange = { },
            readOnly = true,
            label = { Text("Dossier") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Aucun dossier") },
                onClick = {
                    onFolderSelected(null)
                    expanded = false
                }
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text("${folder.name} (${folder.fullPath})") },
                    onClick = {
                        onFolderSelected(folder)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TagSelector(
    tags: List<TagDto>,
    selectedTags: List<TagDto>,
    onTagsChanged: (List<TagDto>) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tags) { tag ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedTags.contains(tag),
                        onCheckedChange = { checked ->
                            if (checked) {
                                onTagsChanged(selectedTags + tag)
                            } else {
                                onTagsChanged(selectedTags - tag)
                            }
                        },
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
