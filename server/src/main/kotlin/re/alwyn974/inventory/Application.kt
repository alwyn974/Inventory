package re.alwyn974.inventory

import com.auth0.jwt.JWT
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktoropenapi.get
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
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import re.alwyn974.inventory.config.AppConfig
import re.alwyn974.inventory.config.appModule
import re.alwyn974.inventory.model.ApiInfoResponse
import re.alwyn974.inventory.model.DocumentationLinks
import re.alwyn974.inventory.model.ErrorResponse
import re.alwyn974.inventory.routes.*
import re.alwyn974.inventory.service.DatabaseFactory
import re.alwyn974.inventory.service.JwtService
import re.alwyn974.inventory.service.MinioService

fun main() {
    val config = AppConfig
    embeddedServer(Netty, port = config.serverPort, host = config.serverHost, module = Application::module).start(wait = true)
}

fun Application.module() {
    // Install Koin for dependency injection
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    // Get services from Koin
    val databaseFactory: DatabaseFactory by inject()
    val jwtService: JwtService by inject()
    val minioService: MinioService by inject()
    val config: AppConfig by inject()

    // Initialize database
    databaseFactory.init()

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
            realm = config.jwtRealm
            verifier(JWT.require(jwtService.algorithm).withIssuer(config.jwtIssuer).withAudience(config.jwtAudience).build())
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
            url = "http://localhost:${config.serverPort}"
            description = "Development server"
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

        get("/docs", {
            description = "API Documentation"
            summary = "Access to API documentation"
            tags("General")
            response {
                HttpStatusCode.OK to "Documentation in HTML format"
            }
        }) {
            call.respondText(
                this::class.java.classLoader.getResource("scalar.html")?.readText() ?: "Documentation not found",
                ContentType.Text.Html
            )
        }

        get("/", {
            description = "API Info"
            summary = "Get API information"
            tags("General")
            response {
                HttpStatusCode.OK to "API information"
            }
        }) {
            call.respond(ApiInfoResponse(
                message = "Inventory API",
                version = "1.0.0",
                status = "running",
                documentation = DocumentationLinks(
                    scalar = "/docs",
                    openapi = "/openapi.json"
                )
            ))
        }

        get("/health", {
            description = "Health check endpoint"
            summary = "Check if the server is healthy"
            tags("General")
            response {
                HttpStatusCode.OK to "Server health status"
            }
        }) {
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