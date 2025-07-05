package re.alwyn974.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import re.alwyn974.inventory.shared.model.CategoryDto
import re.alwyn974.inventory.shared.model.CreateItemRequest
import re.alwyn974.inventory.shared.model.FolderDto
import re.alwyn974.inventory.shared.model.ItemDto
import re.alwyn974.inventory.shared.model.TagDto
import re.alwyn974.inventory.shared.model.UpdateItemRequest

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
    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    var imageUploadError by remember { mutableStateOf<String?>(null) }

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
            errorMessage = "Error loading data: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (item == null) "Create Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Section
            item {
                ImageUploadSection(
                    currentImageUrl = item?.imageUrl,
                    selectedImage = selectedImage,
                    onImageSelected = { imageData ->
                        selectedImage = imageData
                        imageUploadError = null
                    },
                    onImageRemoved = { selectedImage = null },
                    enabled = !isLoading
                )

                imageUploadError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
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
                    label = { Text("Quantity *") },
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
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            println("Debug: Button clicked - name='$name', quantity='$quantity'")
                            if (name.isNotBlank() && quantity.toIntOrNull() != null) {
                                println("Debug: Validation OK, launching request")
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val itemId = if (item == null) {
                                            println("Debug: Creating new item")
                                            val result = apiClient.createItem(
                                                CreateItemRequest(
                                                    name = name,
                                                    description = description.ifBlank { null },
                                                    quantity = quantity.toInt(),
                                                    categoryId = selectedCategory?.id,
                                                    folderId = selectedFolder?.id,
                                                    tagIds = selectedTags.map { it.id }
                                                )
                                            )
                                            println("Debug: Item created successfully: $result")
                                            result["id"] // Get the created item ID
                                        } else {
                                            println("Debug: Updating item ${item.id}")
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
                                            println("Debug: Item updated successfully")
                                            item.id
                                        }

                                        // Upload image if an image was selected
                                        if (selectedImage != null && itemId != null) {
                                            try {
                                                val uploadResult = apiClient.uploadItemImage(
                                                    itemId,
                                                    selectedImage!!,
                                                    "image.jpg"
                                                )
                                                println("Debug: Image uploaded successfully: $uploadResult")
                                            } catch (e: Exception) {
                                                println("Debug: Error uploading image: ${e.message}")
                                                imageUploadError = "Error uploading image: ${e.message}"
                                            }
                                        }

                                        println("Debug: Calling onSaved()")
                                        onSaved()
                                    } catch (e: Exception) {
                                        println("Debug: Error saving: ${e.message}")
                                        e.printStackTrace()
                                        errorMessage = "Error saving: ${e.message}"
                                    } finally {
                                        isLoading = false
                                        println("Debug: End of save process")
                                    }
                                }
                            } else {
                                println("Debug: Validation failed - name='$name' (blank=${name.isBlank()}), quantity='$quantity' (valid=${quantity.toIntOrNull() != null})")
                                // Show error message if validation fails
                                errorMessage = when {
                                    name.isBlank() -> "Name is required"
                                    quantity.toIntOrNull() == null -> "Quantity must be a valid number"
                                    else -> "Please fill in all required fields"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
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
            value = selectedCategory?.name ?: "No category",
            onValueChange = { },
            readOnly = true,
            label = { Text("Category") },
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
                text = { Text("No category") },
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
            value = selectedFolder?.name ?: "No folder",
            onValueChange = { },
            readOnly = true,
            label = { Text("Folder") },
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
                text = { Text("No folder") },
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

@Composable
fun ImageUploadSection(
    currentImageUrl: String?,
    selectedImage: ByteArray?,
    onImageSelected: (ByteArray) -> Unit,
    onImageRemoved: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Item Image",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Display current or selected image
            if (selectedImage != null) {
                // TODO: Display selected image (requires platform-specific implementation)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Selected image",
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Image selected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImageRemoved,
                        enabled = enabled
                    ) {
                        Text("Remove")
                    }

                    // TODO: Add button to change image
                    Button(
                        onClick = { /* TODO: Open image picker */ },
                        enabled = enabled
                    ) {
                        Text("Change")
                    }
                }
            } else if (currentImageUrl != null) {
                AsyncImage(
                    model = currentImageUrl,
                    contentDescription = "Current image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImageRemoved,
                        enabled = enabled
                    ) {
                        Text("Remove")
                    }

                    Button(
                        onClick = { /* TODO: Open image picker */ },
                        enabled = enabled
                    ) {
                        Text("Change")
                    }
                }
            } else {
                // No image - show add button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Add image",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No image",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { /* TODO: Open image sélecteur */ },
                    enabled = enabled
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Image")
                }
            }
        }
    }
}
