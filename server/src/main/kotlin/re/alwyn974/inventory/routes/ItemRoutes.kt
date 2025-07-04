package re.alwyn974.inventory.routes

import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import re.alwyn974.inventory.model.*
import re.alwyn974.inventory.service.MinioService
import re.alwyn974.inventory.shared.model.CategoryDto
import re.alwyn974.inventory.shared.model.CreateItemRequest
import re.alwyn974.inventory.shared.model.ErrorResponse
import re.alwyn974.inventory.shared.model.FolderDto
import re.alwyn974.inventory.shared.model.ItemDto
import re.alwyn974.inventory.shared.model.SuccessResponse
import re.alwyn974.inventory.shared.model.TagDto
import re.alwyn974.inventory.shared.model.UpdateItemRequest
import java.util.*

fun Route.itemRoutes(minioService: MinioService) {
    authenticate("jwt") {
        route("/items") {
            get({
                tags = listOf("Items")
                summary = "List all items"
                description = "Get a list of all inventory items"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "List of items"
                        body<List<ItemDto>>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                getAllItems(call)
            }

            post({
                tags = listOf("Items")
                summary = "Create new item"
                description = "Create a new inventory item"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateItemRequest> {
                        example("example") {
                            value = CreateItemRequest(
                                name = "Laptop Dell XPS 13",
                                description = "High-performance laptop for development",
                                quantity = 5,
                                categoryId = null,
                                folderId = null,
                                tagIds = emptyList()
                            )
                        }
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Item created successfully"
                        body<Map<String, String>>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                createItem(call)
            }

            get("/{id}", {
                tags = listOf("Items")
                summary = "Get item by ID"
                description = "Get a specific inventory item by its ID"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Item information"
                        body<ItemDto>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Item not found"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                getItemById(call)
            }

            patch("/{id}", {
                tags = listOf("Items")
                summary = "Update item"
                description = "Update an existing inventory item (partial update)"
                securitySchemeNames = listOf("JWT")
                request {
                    body<UpdateItemRequest> {
                        example("update") {
                            value = UpdateItemRequest(
                                name = "Updated Laptop Dell XPS 13",
                                description = "Updated high-performance laptop",
                                quantity = 3
                            )
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Item updated successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Item not found"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                updateItem(call)
            }

            delete("/{id}", {
                tags = listOf("Items")
                summary = "Delete item"
                description = "Delete an inventory item"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Item deleted successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Item not found"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                deleteItem(call, minioService)
            }

            post("/{id}/image", {
                tags = listOf("Items")
                summary = "Upload item image"
                description = "Upload an image for an inventory item"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Image uploaded successfully"
                        body<Map<String, String>>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Invalid file type or no image provided"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Item not found"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Insufficient permissions"
                        body<ErrorResponse>()
                    }
                }
            }) {
                uploadItemImage(call, minioService)
            }
        }
    }
}

private suspend fun getAllItems(call: ApplicationCall) {
    call.requirePermission("item.read")

    val items = transaction {
        Items.selectAll().map { itemRow ->
            mapItemRowToDto(itemRow)
        }
    }

    call.respond(items)
}

private suspend fun createItem(call: ApplicationCall) {
    call.requirePermission("item.create")

    val principal = call.principal<JWTPrincipal>()
    val userId = UUID.fromString(principal?.subject!!)
    val createRequest = call.receive<CreateItemRequest>()

    val itemId = transaction {
        val itemId = Items.insert { body ->
            body[name] = createRequest.name
            body[description] = createRequest.description
            body[quantity] = createRequest.quantity
            body[category] = createRequest.categoryId?.let { UUID.fromString(it) }
            body[folder] = createRequest.folderId?.let { UUID.fromString(it) }
            body[createdBy] = userId
        } get Items.id

        // Add tags
        createRequest.tagIds.forEach { tagId ->
            ItemTags.insert {
                it[item] = itemId
                it[tag] = UUID.fromString(tagId)
            }
        }

        itemId
    }

    call.respond(HttpStatusCode.Created, mapOf("id" to itemId.toString()))
}

private suspend fun getItemById(call: ApplicationCall) {
    call.requirePermission("item.read")

    val itemId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest)

    val item = transaction {
        Items.selectAll().where { Items.id eq UUID.fromString(itemId) }.singleOrNull()
    }

    if (item == null) {
        call.respond(HttpStatusCode.NotFound, ErrorResponse("ITEM_NOT_FOUND", "Item not found"))
        return
    }

    val itemDto = mapItemRowToDto(item)
    call.respond(itemDto)
}

private suspend fun updateItem(call: ApplicationCall) {
    call.requirePermission("item.update")

    val itemId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest)
    val updateRequest = call.receive<UpdateItemRequest>()

    val updated = transaction {
        val updateCount = Items.update({ Items.id eq UUID.fromString(itemId) }) {
            updateRequest.name?.let { name -> it[Items.name] = name }
            updateRequest.description?.let { description -> it[Items.description] = description }
            updateRequest.quantity?.let { quantity -> it[Items.quantity] = quantity }
            updateRequest.categoryId?.let { categoryId -> it[Items.category] = UUID.fromString(categoryId) }
            updateRequest.folderId?.let { folderId -> it[Items.folder] = UUID.fromString(folderId) }
            it[Items.updatedAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        }

        // Update tags if provided
        updateRequest.tagIds?.let { tagIds ->
            ItemTags.deleteWhere { ItemTags.item eq UUID.fromString(itemId) }
            tagIds.forEach { tagId ->
                ItemTags.insert {
                    it[item] = UUID.fromString(itemId)
                    it[tag] = UUID.fromString(tagId)
                }
            }
        }

        updateCount
    }

    if (updated == 0) {
        call.respond(HttpStatusCode.NotFound, ErrorResponse("ITEM_NOT_FOUND", "Item not found"))
    } else {
        call.respond(SuccessResponse("Item updated successfully"))
    }
}

private suspend fun deleteItem(call: ApplicationCall, minioService: MinioService) {
    call.requirePermission("item.delete")

    val itemId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest)

    val deleted = transaction {
        // Get image URL before deletion
        val imageUrl = Items.selectAll().where { Items.id eq UUID.fromString(itemId) }
            .singleOrNull()?.get(Items.imageUrl)

        // Delete item tags first
        ItemTags.deleteWhere { ItemTags.item eq UUID.fromString(itemId) }

        // Delete item
        val deleteCount = Items.deleteWhere { Items.id eq UUID.fromString(itemId) }

        // Delete image from MinIO if exists
        if (deleteCount > 0 && imageUrl != null) {
            try {
                minioService.deleteImage(imageUrl)
            } catch (e: Exception) {
                // Log error but don't fail the deletion
            }
        }

        deleteCount
    }

    if (deleted == 0)
        call.respond(HttpStatusCode.NotFound, ErrorResponse("ITEM_NOT_FOUND", "Item not found"))
    else
        call.respond(SuccessResponse("Item deleted successfully"))
}

private suspend fun uploadItemImage(call: ApplicationCall, minioService: MinioService) {
    call.requirePermission("item.update")

    val itemId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest)

    val multipart = call.receiveMultipart()
    var imageUrl: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val name = part.originalFileName ?: "upload"
                val contentType = part.contentType?.toString() ?: "image/jpeg"

                if (!contentType.startsWith("image/")) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_FILE_TYPE", "Only image files are allowed"))
                    return@forEachPart
                }

                val bytes = part.streamProvider().readBytes()
                imageUrl = minioService.uploadImage(bytes, contentType)
            }
            else -> {}
        }
        part.dispose()
    }

    if (imageUrl == null) {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse("NO_IMAGE_PROVIDED", "No image file provided"))
        return
    }

    val updated = transaction {
        // Get old image URL
        val oldImageUrl = Items.selectAll().where { Items.id eq UUID.fromString(itemId) }
            .singleOrNull()?.get(Items.imageUrl)

        // Update item with new image URL
        val updateCount = Items.update({ Items.id eq UUID.fromString(itemId) }) {
            it[Items.imageUrl] = imageUrl
            it[Items.updatedAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        }

        // Delete old image from MinIO if exists
        if (updateCount > 0 && oldImageUrl != null) {
            try {
                minioService.deleteImage(oldImageUrl)
            } catch (e: Exception) {
                // Log error but don't fail the update
            }
        }

        updateCount
    }

    if (updated == 0) {
        call.respond(HttpStatusCode.NotFound, ErrorResponse("ITEM_NOT_FOUND", "Item not found"))
    } else {
        call.respond(mapOf("imageUrl" to imageUrl))
    }
}

private fun mapItemRowToDto(itemRow: ResultRow): ItemDto {
    return transaction {
        val category = itemRow[Items.category]?.let { categoryId ->
            Categories.selectAll().where { Categories.id eq categoryId }.singleOrNull()?.let { categoryRow ->
                CategoryDto(
                    id = categoryRow[Categories.id].toString(),
                    name = categoryRow[Categories.name],
                    description = categoryRow[Categories.description],
                    createdBy = categoryRow[Categories.createdBy].toString(),
                    createdAt = categoryRow[Categories.createdAt].toString(),
                    updatedAt = categoryRow[Categories.updatedAt].toString()
                )
            }
        }

        val folder = itemRow[Items.folder]?.let { folderId ->
            Folders.selectAll().where { Folders.id eq folderId }.singleOrNull()?.let { folderRow ->
                FolderDto(
                    id = folderRow[Folders.id].toString(),
                    name = folderRow[Folders.name],
                    fullPath = folderRow[Folders.fullPath], // Ajout du champ fullPath
                    description = folderRow[Folders.description],
                    parentFolderId = folderRow[Folders.parentFolder]?.toString(),
                    createdBy = folderRow[Folders.createdBy].toString(),
                    createdAt = folderRow[Folders.createdAt].toString(),
                    updatedAt = folderRow[Folders.updatedAt].toString()
                )
            }
        }

        val tags = ItemTags.join(Tags, JoinType.INNER, ItemTags.tag, Tags.id)
            .selectAll().where { ItemTags.item eq itemRow[Items.id] }
            .map { tagRow ->
                TagDto(
                    id = tagRow[Tags.id].toString(),
                    name = tagRow[Tags.name],
                    color = tagRow[Tags.color],
                    createdBy = tagRow[Tags.createdBy].toString(),
                    createdAt = tagRow[Tags.createdAt].toString()
                )
            }

        ItemDto(
            id = itemRow[Items.id].toString(),
            name = itemRow[Items.name],
            description = itemRow[Items.description],
            quantity = itemRow[Items.quantity],
            imageUrl = itemRow[Items.imageUrl],
            category = category,
            folder = folder,
            tags = tags,
            createdBy = itemRow[Items.createdBy].toString(),
            createdAt = itemRow[Items.createdAt].toString(),
            updatedAt = itemRow[Items.updatedAt].toString()
        )
    }
}
