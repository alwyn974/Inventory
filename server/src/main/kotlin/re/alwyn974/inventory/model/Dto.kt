package re.alwyn974.inventory.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import java.util.UUID

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
data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val role: UserRole? = null,
    val isActive: Boolean? = null
)

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
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String?
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
    val color: String?,
    val createdBy: String,
    val createdAt: String
)

@Serializable
data class CreateTagRequest(
    val name: String,
    val color: String?
)

@Serializable
data class FolderDto(
    val id: String,
    val name: String,
    val fullPath: String, // Nouveau champ pour le chemin complet
    val description: String?,
    val parentFolderId: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateFolderRequest(
    val name: String,
    val fullPath: String, // Nouveau champ pour le chemin complet
    val description: String?,
    val parentFolderId: String?
)

@Serializable
data class UpdateFolderRequest(
    val name: String? = null,
    val fullPath: String? = null, // Nouveau champ pour le chemin complet
    val description: String? = null,
    val parentFolderId: String? = null
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val description: String?,
    val quantity: Int,
    val imageUrl: String?,
    val category: CategoryDto?,
    val folder: FolderDto?,
    val tags: List<TagDto>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String?,
    val quantity: Int = 0,
    val categoryId: String?,
    val folderId: String?,
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
data class PermissionDto(
    val id: String,
    val name: String,
    val description: String?
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

@Serializable
data class ApiInfoResponse(
    val message: String,
    val version: String,
    val status: String,
    val documentation: DocumentationLinks
)

@Serializable
data class DocumentationLinks(
    val scalar: String,
    val openapi: String
)
