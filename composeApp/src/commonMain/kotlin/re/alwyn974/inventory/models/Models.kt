package re.alwyn974.inventory.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
enum class UserRole {
    ADMIN, MANAGER, USER, VIEWER
}

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val quantity: Int,
    val imageUrl: String? = null,
    val category: CategoryDto? = null,
    val folder: FolderDto? = null,
    val tags: List<TagDto> = emptyList(),
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String? = null,
    val quantity: Int,
    val categoryId: String? = null,
    val folderId: String? = null,
    val tagIds: List<String> = emptyList()
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    val categoryId: String? = null,
    val folderId: String? = null,
    val tagIds: List<String>? = null
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdBy: String,
    val createdAt: String
)

@Serializable
data class CreateTagRequest(
    val name: String,
    val color: String? = null
)

@Serializable
data class FolderDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val fullPath: String,
    val parentFolderId: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateFolderRequest(
    val name: String,
    val description: String? = null,
    val fullPath: String,
    val parentFolderId: String? = null
)

@Serializable
data class UpdateFolderRequest(
    val name: String? = null,
    val description: String? = null,
    val fullPath: String? = null,
    val parentFolderId: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

@Serializable
data class SuccessResponse(
    val message: String
)
