package re.alwyn974.inventory.routes

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
            get {
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

            post {
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

            get("/{id}") {
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

            put("/{id}") {
                call.requirePermission("category.update")

                val categoryId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
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

            delete("/{id}") {
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
            get {
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

            post {
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

            delete("/{id}") {
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
            get {
                call.requirePermission("folder.read")

                val folders = transaction {
                    Folders.selectAll().map { row ->
                        FolderDto(
                            id = row[Folders.id].toString(),
                            name = row[Folders.name],
                            fullPath = row[Folders.fullPath], // Ajout du champ fullPath
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

            post {
                call.requirePermission("folder.create")

                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.subject!!)
                val createRequest = call.receive<CreateFolderRequest>()

                val folderId = transaction {
                    Folders.insert {
                        it[name] = createRequest.name
                        it[fullPath] = createRequest.fullPath // Ajout du champ fullPath
                        it[description] = createRequest.description
                        it[parentFolder] = createRequest.parentFolderId?.let { UUID.fromString(it) }
                        it[createdBy] = userId
                    } get Folders.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to folderId.toString()))
            }

            get("/{id}") {
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
                    fullPath = folder[Folders.fullPath], // Ajout du champ fullPath
                    description = folder[Folders.description],
                    parentFolderId = folder[Folders.parentFolder]?.toString(),
                    createdBy = folder[Folders.createdBy].toString(),
                    createdAt = folder[Folders.createdAt].toString(),
                    updatedAt = folder[Folders.updatedAt].toString()
                )

                call.respond(folderDto)
            }

            put("/{id}") {
                call.requirePermission("folder.update")

                val folderId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val updateRequest = call.receive<UpdateFolderRequest>()

                val updated = transaction {
                    Folders.update({ Folders.id eq UUID.fromString(folderId) }) {
                        updateRequest.name?.let { name -> it[Folders.name] = name }
                        updateRequest.fullPath?.let { fullPath -> it[Folders.fullPath] = fullPath } // Ajout du champ fullPath
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

            delete("/{id}") {
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
