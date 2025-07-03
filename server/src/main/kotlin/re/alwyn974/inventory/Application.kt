package re.alwyn974.inventory

import com.auth0.jwt.JWT
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.openApi
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import re.alwyn974.inventory.model.ErrorResponse
import re.alwyn974.inventory.routes.*
import re.alwyn974.inventory.service.DatabaseFactory
import re.alwyn974.inventory.service.JwtService
import re.alwyn974.inventory.service.MinioService

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Initialize MinIO service
    val minioService = MinioService(
        endpoint = System.getenv("MINIO_ENDPOINT") ?: "http://localhost:9000",
        accessKey = System.getenv("MINIO_ACCESS_KEY") ?: "minioadmin",
        secretKey = System.getenv("MINIO_SECRET_KEY") ?: "minioadmin",
        bucketName = System.getenv("MINIO_BUCKET_NAME") ?: "inventory-images"
    )

    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", cause.message ?: "Unknown error")
            )
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                ErrorResponse("UNAUTHORIZED", "Authentication required")
            )
        }

        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(
                status,
                ErrorResponse("FORBIDDEN", "Access denied")
            )
        }
    }

    install(Authentication) {
        jwt("jwt") {
            realm = "Inventory Application"
            verifier(JWT.require(JwtService.algorithm).withIssuer(JwtService.ISSUER).build())
            validate { credential ->
                val userId = credential.payload.subject
                val username = credential.payload.getClaim("username").asString()
                val role = credential.payload.getClaim("role").asString()

                if (userId != null && username != null && role != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    install(OpenApi) {
        info {
            title = "Inventory API"
            version = "1.0.0"
            description = "API for managing inventory items, categories, tags, and folders."
        }
        server {
            url = "http://localhost:$SERVER_PORT"
            description = "Serveur de développement"
        }
        security {
            securityScheme("JWT") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "JWT"
            }
        }
    }

    // Configure routing
    routing {
        route("openapi.json") {
            openApi()
        }

        // Scalar UI (interface moderne)
        get("/docs") {
            call.respondText(
                this::class.java.classLoader.getResource("scalar.html")?.readText() ?: "Documentation not found",
                ContentType.Text.Html
            )
        }

        get("/") {
            call.respond(mapOf(
                "message" to "Inventory API",
                "version" to "1.0.0",
                "status" to "running",
                "documentation" to mapOf(
                    "scalar" to "/docs",
                    "swagger" to "/swagger",
                    "openapi" to "/openapi.json"
                )
            ))
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }

        // API routes
        route("/api/v1") {
            authRoutes()
            userRoutes()
            itemRoutes(minioService)
            categoryRoutes()
            tagRoutes()
            folderRoutes()
        }
    }
}