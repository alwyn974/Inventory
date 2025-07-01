package re.alwyn974.inventory.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 20, UserRole::class)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object Categories : UUIDTable("categories") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object Tags : UUIDTable("tags") {
    val name = varchar("name", 50)
    val color = varchar("color", 7).nullable() // Hex color code
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object Folders : UUIDTable("folders") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val parentFolder = reference("parent_folder", Folders).nullable()
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object Items : UUIDTable("items") {
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val quantity = integer("quantity").default(0)
    val minQuantity = integer("min_quantity").default(0)
    val imageUrl = varchar("image_url", 500).nullable()
    val category = reference("category", Categories).nullable()
    val folder = reference("folder", Folders).nullable()
    val createdBy = reference("created_by", Users)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object ItemTags : UUIDTable("item_tags") {
    val item = reference("item_id", Items)
    val tag = reference("tag_id", Tags)

    init {
        uniqueIndex(item, tag)
    }
}

object Permissions : UUIDTable("permissions") {
    val name = varchar("name", 50).uniqueIndex()
    val description = text("description").nullable()
}

object RolePermissions : UUIDTable("role_permissions") {
    val role = enumerationByName("role", 20, UserRole::class)
    val permission = reference("permission", Permissions)

    init {
        uniqueIndex(role, permission)
    }
}

enum class UserRole {
    ADMIN,
    MANAGER,
    USER,
    VIEWER
}
