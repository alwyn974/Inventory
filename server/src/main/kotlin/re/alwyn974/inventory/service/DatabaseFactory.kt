package re.alwyn974.inventory.service

import com.oracle.graal.compiler.enterprise.phases.CountedStripMiningPhase.db
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import re.alwyn974.inventory.model.*
import re.alwyn974.inventory.model.Users.username

object DatabaseFactory {
    fun init() {
        val driverClassName = System.getenv("DATABASE_DRIVER") ?: "org.h2.Driver"
        val jdbcURL = System.getenv("DATABASE_URL") ?: "jdbc:h2:./inventory;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

        val database = Database.connect(createHikariDataSource(jdbcURL, driverClassName))

        transaction(database) {
            // Create tables
            SchemaUtils.create(
                Users,
                Categories,
                Tags,
                Folders,
                Items,
                ItemTags,
                Permissions,
                RolePermissions
            )

            // Insert default permissions
            initializePermissions()

            // Create default admin user
            createDefaultAdmin()
        }
    }

    private fun createHikariDataSource(url: String, driver: String) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    private fun initializePermissions() {
        val permissions = listOf(
            "item.create" to "Create new items",
            "item.read" to "View items",
            "item.update" to "Update existing items",
            "item.delete" to "Delete items",
            "category.create" to "Create new categories",
            "category.read" to "View categories",
            "category.update" to "Update existing categories",
            "category.delete" to "Delete categories",
            "tag.create" to "Create new tags",
            "tag.read" to "View tags",
            "tag.update" to "Update existing tags",
            "tag.delete" to "Delete tags",
            "folder.create" to "Create new folders",
            "folder.read" to "View folders",
            "folder.update" to "Update existing folders",
            "folder.delete" to "Delete folders",
            "user.create" to "Create new users",
            "user.read" to "View users",
            "user.update" to "Update existing users",
            "user.delete" to "Delete users"
        )

        permissions.forEach { (name, description) ->
            if (Permissions.selectAll().where { Permissions.name eq name }.empty()) {
                Permissions.insert {
                    it[Permissions.name] = name
                    it[Permissions.description] = description
                }
            }
        }

        // Set up role permissions
        setupRolePermissions()
    }

    private fun setupRolePermissions() {
        val adminPermissions = Permissions.selectAll().map { it[Permissions.id] }
        val managerPermissions = Permissions.selectAll().where { Permissions.name notLike "user.%" }.map { it[Permissions.id] }
        val userPermissions = Permissions.selectAll().where {
            Permissions.name like "%.read" or (Permissions.name like "item.%") or
                    (Permissions.name like "category.%") or (Permissions.name like "tag.%") or
                    (Permissions.name like "folder.%")
        }.map { it[Permissions.id] }
        val viewerPermissions = Permissions.selectAll().where { Permissions.name like "%.read" }.map { it[Permissions.id] }

        // Clear existing role permissions
        RolePermissions.deleteAll()

        // Set ADMIN permissions
        adminPermissions.forEach { permissionId ->
            RolePermissions.insert {
                it[role] = UserRole.ADMIN
                it[permission] = permissionId
            }
        }

        // Set MANAGER permissions
        managerPermissions.forEach { permissionId ->
            RolePermissions.insert {
                it[role] = UserRole.MANAGER
                it[permission] = permissionId
            }
        }

        // Set USER permissions
        userPermissions.forEach { permissionId ->
            RolePermissions.insert {
                it[role] = UserRole.USER
                it[permission] = permissionId
            }
        }

        // Set VIEWER permissions
        viewerPermissions.forEach { permissionId ->
            RolePermissions.insert {
                it[role] = UserRole.VIEWER
                it[permission] = permissionId
            }
        }
    }

    private fun createDefaultAdmin() {
        if (Users.selectAll().where { Users.username eq "admin" }.empty()) {
            Users.insert {
                it[username] = "admin"
                it[email] = "admin@inventory.local"
                it[passwordHash] = PasswordService.hashPassword("admin123")
                it[role] = UserRole.ADMIN
                it[isActive] = true
            }
        }
    }
}
