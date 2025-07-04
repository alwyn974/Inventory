package re.alwyn974.inventory.routes

import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import re.alwyn974.inventory.model.*
import java.util.*

fun Route.categoryRoutes() {
    authenticate("jwt") {
        route("/categories") {
            get({
                tags = listOf("Categories")
                summary = "List all categories"
                description = "Get a list of all categories"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "List of categories"
                        body<List<CategoryDto>>()
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
                call.requirePermission("category.read")

                val categories = transaction {
                    Categories.selectAll().map { row ->
                        CategoryDto(
                            id = row[Categories.id].toString(),
                            name = row[Categories.name],
                            description = row[Categories.description],
                            createdBy = row[Categories.createdBy].toString(),
                            createdAt = row[Categories.createdAt].toString(),
                            updatedAt = row[Categories.updatedAt].toString()
                        )
                    }
                }

                call.respond(categories)
            }

            post({
                tags = listOf("Categories")
                summary = "Create new category"
                description = "Create a new category for organizing items"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateCategoryRequest> {
                        example("electronics") {
                            value = CreateCategoryRequest("Electronics", "Electronic devices and components")
                        }
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Category created successfully"
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
                call.requirePermission("category.create")

                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.subject!!)
                val createRequest = call.receive<CreateCategoryRequest>()

                val categoryId = transaction {
                    Categories.insert {
                        it[name] = createRequest.name
                        it[description] = createRequest.description
                        it[createdBy] = userId
                    } get Categories.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to categoryId.toString()))
            }

            get("/{id}", {
                tags = listOf("Categories")
                summary = "Get category by ID"
                description = "Get a specific category by its ID"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Category information"
                        body<CategoryDto>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Category not found"
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
                call.requirePermission("category.read")

                val categoryId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val category = transaction {
                    Categories.selectAll().where { Categories.id eq UUID.fromString(categoryId) }.singleOrNull()
                }

                if (category == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("CATEGORY_NOT_FOUND", "Category not found"))
                    return@get
                }

                val categoryDto = CategoryDto(
                    id = category[Categories.id].toString(),
                    name = category[Categories.name],
                    description = category[Categories.description],
                    createdBy = category[Categories.createdBy].toString(),
                    createdAt = category[Categories.createdAt].toString(),
                    updatedAt = category[Categories.updatedAt].toString()
                )

                call.respond(categoryDto)
            }

            patch("/{id}", {
                tags = listOf("Categories")
                summary = "Update category"
                description = "Update an existing category (partial update)"
                securitySchemeNames = listOf("JWT")
                request {
                    body<UpdateCategoryRequest> {
                        example("update") {
                            value = UpdateCategoryRequest("Updated Electronics", "Updated electronic devices and components")
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Category updated successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Category not found"
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
                call.requirePermission("category.update")

                val categoryId = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val updateRequest = call.receive<UpdateCategoryRequest>()

                val updated = transaction {
                    Categories.update({ Categories.id eq UUID.fromString(categoryId) }) {
                        updateRequest.name?.let { name -> it[Categories.name] = name }
                        updateRequest.description?.let { description -> it[Categories.description] = description }
                        it[Categories.updatedAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("CATEGORY_NOT_FOUND", "Category not found"))
                } else {
                    call.respond(SuccessResponse("Category updated successfully"))
                }
            }

            delete("/{id}", {
                tags = listOf("Categories")
                summary = "Delete category"
                description = "Delete a category from the system"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Category deleted successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Category not found"
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
                call.requirePermission("category.delete")

                val categoryId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val deleted = transaction {
                    Categories.deleteWhere { Categories.id eq UUID.fromString(categoryId) }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("CATEGORY_NOT_FOUND", "Category not found"))
                } else {
                    call.respond(SuccessResponse("Category deleted successfully"))
                }
            }
        }
    }
}

fun Route.tagRoutes() {
    authenticate("jwt") {
        route("/tags") {
            get({
                tags = listOf("Tags")
                summary = "List all tags"
                description = "Get a list of all tags"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "List of tags"
                        body<List<TagDto>>()
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
                call.requirePermission("tag.read")

                val tags = transaction {
                    Tags.selectAll().map { row ->
                        TagDto(
                            id = row[Tags.id].toString(),
                            name = row[Tags.name],
                            color = row[Tags.color],
                            createdBy = row[Tags.createdBy].toString(),
                            createdAt = row[Tags.createdAt].toString()
                        )
                    }
                }

                call.respond(tags)
            }

            post({
                tags = listOf("Tags")
                summary = "Create new tag"
                description = "Create a new tag for labeling items"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateTagRequest> {
                        example("urgent") {
                            value = CreateTagRequest("Urgent", "#FF0000")
                        }
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Tag created successfully"
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
                call.requirePermission("tag.create")

                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.subject!!)
                val createRequest = call.receive<CreateTagRequest>()

                val tagId = transaction {
                    Tags.insert {
                        it[name] = createRequest.name
                        it[color] = createRequest.color
                        it[createdBy] = userId
                    } get Tags.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to tagId.toString()))
            }

            get("/{id}", {
                tags = listOf("Tags")
                summary = "Get tag by ID"
                description = "Get a specific tag by its ID"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Tag information"
                        body<TagDto>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Tag not found"
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
                call.requirePermission("tag.read")

                val tagId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val tag = transaction {
                    Tags.selectAll().where { Tags.id eq UUID.fromString(tagId) }.singleOrNull()
                }

                if (tag == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("TAG_NOT_FOUND", "Tag not found"))
                    return@get
                }

                val tagDto = TagDto(
                    id = tag[Tags.id].toString(),
                    name = tag[Tags.name],
                    color = tag[Tags.color],
                    createdBy = tag[Tags.createdBy].toString(),
                    createdAt = tag[Tags.createdAt].toString()
                )

                call.respond(tagDto)
            }

            patch("/{id}", {
                tags = listOf("Tags")
                summary = "Update tag"
                description = "Update an existing tag (partial update)"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateTagRequest> {
                        example("update") {
                            value = CreateTagRequest("Updated Urgent", "#FF6600")
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Tag updated successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Tag not found"
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
                call.requirePermission("tag.update")

                val tagId = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val updateRequest = call.receive<CreateTagRequest>()

                val updated = transaction {
                    Tags.update({ Tags.id eq UUID.fromString(tagId) }) {
                        it[name] = updateRequest.name
                        it[color] = updateRequest.color
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("TAG_NOT_FOUND", "Tag not found"))
                } else {
                    call.respond(SuccessResponse("Tag updated successfully"))
                }
            }

            delete("/{id}", {
                tags = listOf("Tags")
                summary = "Delete tag"
                description = "Delete a tag from the system"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Tag deleted successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Tag not found"
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
                call.requirePermission("tag.delete")

                val tagId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val deleted = transaction {
                    // Remove tag associations first
                    ItemTags.deleteWhere { ItemTags.tag eq UUID.fromString(tagId) }

                    // Delete tag
                    Tags.deleteWhere { Tags.id eq UUID.fromString(tagId) }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("TAG_NOT_FOUND", "Tag not found"))
                } else {
                    call.respond(SuccessResponse("Tag deleted successfully"))
                }
            }
        }
    }
}

fun Route.folderRoutes() {
    authenticate("jwt") {
        route("/folders") {
            get({
                tags = listOf("Folders")
                summary = "List all folders"
                description = "Get a list of all folders with their full paths"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "List of folders"
                        body<List<FolderDto>>()
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
                call.requirePermission("folder.read")

                val folders = transaction {
                    Folders.selectAll().map { row ->
                        FolderDto(
                            id = row[Folders.id].toString(),
                            name = row[Folders.name],
                            fullPath = row[Folders.fullPath],
                            description = row[Folders.description],
                            parentFolderId = row[Folders.parentFolder]?.toString(),
                            createdBy = row[Folders.createdBy].toString(),
                            createdAt = row[Folders.createdAt].toString(),
                            updatedAt = row[Folders.updatedAt].toString()
                        )
                    }
                }

                call.respond(folders)
            }

            post({
                tags = listOf("Folders")
                summary = "Create new folder"
                description = "Create a new folder for organizing items in MinIO and mobile app"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateFolderRequest> {
                        example("warehouse") {
                            value = CreateFolderRequest(
                                name = "Warehouse A",
                                fullPath = "/storage/warehouse-a",
                                description = "Main warehouse storage area",
                                parentFolderId = null
                            )
                        }
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Folder created successfully"
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
                call.requirePermission("folder.create")

                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.subject!!)
                val createRequest = call.receive<CreateFolderRequest>()

                val folderId = transaction {
                    Folders.insert {
                        it[name] = createRequest.name
                        it[fullPath] = createRequest.fullPath
                        it[description] = createRequest.description
                        it[parentFolder] = createRequest.parentFolderId?.let { UUID.fromString(it) }
                        it[createdBy] = userId
                    } get Folders.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to folderId.toString()))
            }

            get("/{id}", {
                tags = listOf("Folders")
                summary = "Get folder by ID"
                description = "Get a specific folder by its ID with full path information"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Folder information"
                        body<FolderDto>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Folder not found"
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
                call.requirePermission("folder.read")

                val folderId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val folder = transaction {
                    Folders.selectAll().where { Folders.id eq UUID.fromString(folderId) }.singleOrNull()
                }

                if (folder == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("FOLDER_NOT_FOUND", "Folder not found"))
                    return@get
                }

                val folderDto = FolderDto(
                    id = folder[Folders.id].toString(),
                    name = folder[Folders.name],
                    fullPath = folder[Folders.fullPath],
                    description = folder[Folders.description],
                    parentFolderId = folder[Folders.parentFolder]?.toString(),
                    createdBy = folder[Folders.createdBy].toString(),
                    createdAt = folder[Folders.createdAt].toString(),
                    updatedAt = folder[Folders.updatedAt].toString()
                )

                call.respond(folderDto)
            }

            patch("/{id}", {
                tags = listOf("Folders")
                summary = "Update folder"
                description = "Update an existing folder including its full path (partial update)"
                securitySchemeNames = listOf("JWT")
                request {
                    body<UpdateFolderRequest> {
                        example("update") {
                            value = UpdateFolderRequest(
                                name = "Updated Warehouse A",
                                fullPath = "/storage/updated-warehouse-a",
                                description = "Updated main warehouse storage area"
                            )
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Folder updated successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Folder not found"
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
                call.requirePermission("folder.update")

                val folderId = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val updateRequest = call.receive<UpdateFolderRequest>()

                val updated = transaction {
                    Folders.update({ Folders.id eq UUID.fromString(folderId) }) {
                        updateRequest.name?.let { name -> it[Folders.name] = name }
                        updateRequest.fullPath?.let { fullPath -> it[Folders.fullPath] = fullPath }
                        updateRequest.description?.let { description -> it[Folders.description] = description }
                        updateRequest.parentFolderId?.let { parentId -> it[Folders.parentFolder] = UUID.fromString(parentId) }
                        it[Folders.updatedAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("FOLDER_NOT_FOUND", "Folder not found"))
                } else {
                    call.respond(SuccessResponse("Folder updated successfully"))
                }
            }

            delete("/{id}", {
                tags = listOf("Folders")
                summary = "Delete folder"
                description = "Delete a folder from the system"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "Folder deleted successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "Folder not found"
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
                call.requirePermission("folder.delete")

                val folderId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val deleted = transaction {
                    Folders.deleteWhere { Folders.id eq UUID.fromString(folderId) }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("FOLDER_NOT_FOUND", "Folder not found"))
                } else {
                    call.respond(SuccessResponse("Folder deleted successfully"))
                }
            }
        }
    }
}
