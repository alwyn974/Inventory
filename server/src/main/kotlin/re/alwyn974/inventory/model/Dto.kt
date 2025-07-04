package re.alwyn974.inventory.model

import kotlinx.serialization.Serializable

@Serializable
data class PermissionDto(
    val id: String,
    val name: String,
    val description: String?
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
