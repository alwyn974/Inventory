package re.alwyn974.inventory.routes

import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.delete
import io.ktor.http.*
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
import org.koin.ktor.ext.inject
import re.alwyn974.inventory.model.Permissions
import re.alwyn974.inventory.model.RolePermissions
import re.alwyn974.inventory.model.Users
import re.alwyn974.inventory.model.Users.email
import re.alwyn974.inventory.model.Users.username
import re.alwyn974.inventory.service.JwtService
import re.alwyn974.inventory.service.PasswordService
import re.alwyn974.inventory.service.RefreshTokenService
import re.alwyn974.inventory.shared.model.CreateUserRequest
import re.alwyn974.inventory.shared.model.ErrorResponse
import re.alwyn974.inventory.shared.model.LoginRequest
import re.alwyn974.inventory.shared.model.LoginResponse
import re.alwyn974.inventory.shared.model.RefreshTokenRequest
import re.alwyn974.inventory.shared.model.RefreshTokenResponse
import re.alwyn974.inventory.shared.model.SuccessResponse
import re.alwyn974.inventory.shared.model.UpdateUserRequest
import re.alwyn974.inventory.shared.model.UserDto
import re.alwyn974.inventory.shared.model.UserRole
import java.util.*

fun Route.authRoutes() {
    // Koin dependency injection
    val jwtService by inject<JwtService>()
    val passwordService by inject<PasswordService>()
    val refreshTokenService by inject<RefreshTokenService>()

    route("/auth") {
        post("/login", {
            tags = listOf("Authentication")
            summary = "User login"
            description = "Authenticate user and get access and refresh tokens"
            request {
                body<LoginRequest> {
                    example("default") {
                        value = LoginRequest("admin", "admin123")
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Login successful"
                    body<LoginResponse>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Invalid credentials"
                    body<ErrorResponse>()
                }
                HttpStatusCode.Forbidden to {
                    description = "Account disabled"
                    body<ErrorResponse>()
                }
            }
        }) {
            val loginRequest = call.receive<LoginRequest>()

            val user = transaction {
                Users.selectAll().where { Users.username eq loginRequest.username }.singleOrNull()
            }

            if (user == null || !passwordService.verifyPassword(loginRequest.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"))
                return@post
            }

            if (!user[Users.isActive]) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("ACCOUNT_DISABLED", "Account is disabled"))
                return@post
            }

            val userId = user[Users.id].value
            val deviceInfo = call.request.headers["User-Agent"]

            val accessToken = jwtService.generateAccessToken(
                userId = userId.toString(),
                username = user[Users.username],
                role = user[Users.role]
            )

            val refreshToken = refreshTokenService.generateRefreshToken(userId, deviceInfo)

            val userDto = UserDto(
                id = user[Users.id].toString(),
                username = user[username],
                email = user[email],
                role = user[Users.role],
                isActive = user[Users.isActive],
                createdAt = user[Users.createdAt].toString(),
                updatedAt = user[Users.updatedAt].toString()
            )

            call.respond(LoginResponse(accessToken, refreshToken, userDto))
        }

        post("/refresh", {
            tags = listOf("Authentication")
            summary = "Refresh access token"
            description = "Get a new access token using a refresh token"
            request {
                body<RefreshTokenRequest> {
                    example("default") {
                        value = RefreshTokenRequest("your-refresh-token-here")
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Token refresh successful"
                    body<RefreshTokenResponse>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Invalid or expired refresh token"
                    body<ErrorResponse>()
                }
            }
        }) {
            val refreshRequest = call.receive<RefreshTokenRequest>()

            val userId = refreshTokenService.validateAndUseRefreshToken(refreshRequest.refreshToken)

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token"))
                return@post
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq userId }.singleOrNull()
            }

            if (user == null || !user[Users.isActive]) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("USER_NOT_FOUND", "User not found or inactive"))
                return@post
            }

            val deviceInfo = call.request.headers["User-Agent"]

            val newAccessToken = jwtService.generateAccessToken(
                userId = userId.toString(),
                username = user[Users.username],
                role = user[Users.role]
            )

            val newRefreshToken = refreshTokenService.generateRefreshToken(userId, deviceInfo)

            call.respond(RefreshTokenResponse(newAccessToken, newRefreshToken))
        }

        post("/logout", {
            tags = listOf("Authentication")
            summary = "User logout"
            description = "Logout user and revoke refresh token"
            request {
                body<RefreshTokenRequest> {
                    example("default") {
                        value = RefreshTokenRequest("your-refresh-token-here")
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Logout successful"
                    body<SuccessResponse>()
                }
            }
        }) {
            val logoutRequest = call.receive<RefreshTokenRequest>()
            refreshTokenService.revokeRefreshToken(logoutRequest.refreshToken)
            call.respond(SuccessResponse("Logged out successfully"))
        }

        authenticate("jwt") {
            get("/me", {
                tags = listOf("Authentication")
                summary = "Get current user"
                description = "Get information about the currently authenticated user"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "User information"
                        body<UserDto>()
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Authentication required"
                        body<ErrorResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                        body<ErrorResponse>()
                    }
                }
            }) {
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
                    username = user[username],
                    email = user[email],
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
            get({
                tags = listOf("Users")
                summary = "List all users"
                description = "Get a list of all users in the system"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "List of users"
                        body<List<UserDto>>()
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
                call.requirePermission("user.read")

                val users = transaction {
                    Users.selectAll().map { row ->
                        UserDto(
                            id = row[Users.id].toString(),
                            username = row[username],
                            email = row[email],
                            role = row[Users.role],
                            isActive = row[Users.isActive],
                            createdAt = row[Users.createdAt].toString(),
                            updatedAt = row[Users.updatedAt].toString()
                        )
                    }
                }

                call.respond(users)
            }

            post({
                tags = listOf("Users")
                summary = "Create new user"
                description = "Create a new user account"
                securitySchemeNames = listOf("JWT")
                request {
                    body<CreateUserRequest> {
                        example("admin") {
                            value = CreateUserRequest("newuser", "user@example.com", "password123", UserRole.USER)
                        }
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "User created successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.Conflict to {
                        description = "Username or email already exists"
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

            get("/{id}", {
                tags = listOf("Users")
                summary = "Get user by ID"
                description = "Get a specific user by their ID"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "User information"
                        body<UserDto>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
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
                    username = user[username],
                    email = user[email],
                    role = user[Users.role],
                    isActive = user[Users.isActive],
                    createdAt = user[Users.createdAt].toString(),
                    updatedAt = user[Users.updatedAt].toString()
                )

                call.respond(userDto)
            }

            patch("/{id}", {
                tags = listOf("Users")
                summary = "Update user"
                description = "Update an existing user (partial update)"
                securitySchemeNames = listOf("JWT")
                request {
                    body<UpdateUserRequest> {
                        example("update") {
                            value = UpdateUserRequest(username = "updateduser", email = "updated@example.com")
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "User updated successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
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
                call.requirePermission("user.update")

                val userId = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
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

            delete("/{id}", {
                tags = listOf("Users")
                summary = "Delete user"
                description = "Delete a user from the system"
                securitySchemeNames = listOf("JWT")
                response {
                    HttpStatusCode.OK to {
                        description = "User deleted successfully"
                        body<SuccessResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
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
