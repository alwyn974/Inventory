package re.alwyn974.inventory.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import re.alwyn974.inventory.model.*
import re.alwyn974.inventory.service.JwtService
import re.alwyn974.inventory.service.PasswordService
import java.util.*

fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            val loginRequest = call.receive<LoginRequest>()

            val user = transaction {
                Users.selectAll().where { Users.username eq loginRequest.username }.singleOrNull()
            }

            if (user == null || !PasswordService.verifyPassword(loginRequest.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"))
                return@post
            }

            if (!user[Users.isActive]) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("ACCOUNT_DISABLED", "Account is disabled"))
                return@post
            }

            val token = JwtService.generateToken(
                userId = user[Users.id].toString(),
                username = user[Users.username],
                role = user[Users.role]
            )

            val userDto = UserDto(
                id = user[Users.id].toString(),
                username = user[Users.username],
                email = user[Users.email],
                role = user[Users.role],
                isActive = user[Users.isActive],
                createdAt = user[Users.createdAt].toString(),
                updatedAt = user[Users.updatedAt].toString()
            )

            call.respond(LoginResponse(token, userDto))
        }

        authenticate("jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val user = transaction {
                    Users.selectAll().where { Users.id eq UUID.fromString(userId) }.singleOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                    return@get
                }

                val userDto = UserDto(
                    id = user[Users.id].toString(),
                    username = user[Users.username],
                    email = user[Users.email],
                    role = user[Users.role],
                    isActive = user[Users.isActive],
                    createdAt = user[Users.createdAt].toString(),
                    updatedAt = user[Users.updatedAt].toString()
                )

                call.respond(userDto)
            }
        }
    }
}

fun Route.userRoutes() {
    authenticate("jwt") {
        route("/users") {
            get {
                call.requirePermission("user.read")

                val users = transaction {
                    Users.selectAll().map { row ->
                        UserDto(
                            id = row[Users.id].toString(),
                            username = row[Users.username],
                            email = row[Users.email],
                            role = row[Users.role],
                            isActive = row[Users.isActive],
                            createdAt = row[Users.createdAt].toString(),
                            updatedAt = row[Users.updatedAt].toString()
                        )
                    }
                }

                call.respond(users)
            }

            post {
                call.requirePermission("user.create")

                val createRequest = call.receive<CreateUserRequest>()

                // Check if username or email already exists
                val existingUser = transaction {
                    Users.selectAll().where { (Users.username eq createRequest.username) or (Users.email eq createRequest.email) }.singleOrNull()
                }

                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("USER_EXISTS", "Username or email already exists"))
                    return@post
                }

                transaction {
                    Users.insert {
                        it[username] = createRequest.username
                        it[email] = createRequest.email
                        it[passwordHash] = PasswordService.hashPassword(createRequest.password)
                        it[role] = createRequest.role
                    } get Users.id
                }

                call.respond(HttpStatusCode.Created, SuccessResponse("User created successfully"))
            }

            get("/{id}") {
                call.requirePermission("user.read")

                val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val user = transaction {
                    Users.selectAll().where { Users.id eq UUID.fromString(userId) }
                        .singleOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                    return@get
                }

                val userDto = UserDto(
                    id = user[Users.id].toString(),
                    username = user[Users.username],
                    email = user[Users.email],
                    role = user[Users.role],
                    isActive = user[Users.isActive],
                    createdAt = user[Users.createdAt].toString(),
                    updatedAt = user[Users.updatedAt].toString()
                )

                call.respond(userDto)
            }

            put("/{id}") {
                call.requirePermission("user.update")

                val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val updateRequest = call.receive<UpdateUserRequest>()

                val updated = transaction {
                    Users.update({ Users.id eq UUID.fromString(userId) }) {
                        updateRequest.username?.let { username -> it[Users.username] = username }
                        updateRequest.email?.let { email -> it[Users.email] = email }
                        updateRequest.password?.let { password -> it[Users.passwordHash] = PasswordService.hashPassword(password) }
                        updateRequest.role?.let { role -> it[Users.role] = role }
                        updateRequest.isActive?.let { isActive -> it[Users.isActive] = isActive }
                        it[Users.updatedAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                } else {
                    call.respond(SuccessResponse("User updated successfully"))
                }
            }

            delete("/{id}") {
                call.requirePermission("user.delete")

                val userId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val deleted = transaction {
                    Users.deleteWhere { Users.id eq UUID.fromString(userId) }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                } else {
                    call.respond(SuccessResponse("User deleted successfully"))
                }
            }
        }
    }
}

suspend fun ApplicationCall.requirePermission(permission: String) {
    val principal = principal<JWTPrincipal>()
    val userRole = principal?.getClaim("role", String::class)?.let { UserRole.valueOf(it) }

    if (userRole == null) {
        respond(HttpStatusCode.Unauthorized)
        return
    }

    val hasPermission = transaction {
        RolePermissions.join(Permissions, JoinType.INNER, RolePermissions.permission, Permissions.id)
            .selectAll().where { (RolePermissions.role eq userRole) and (Permissions.name eq permission) }
            .count() > 0
    }

    if (!hasPermission) {
        respond(HttpStatusCode.Forbidden, ErrorResponse("INSUFFICIENT_PERMISSIONS", "Insufficient permissions"))
        return
    }
}
