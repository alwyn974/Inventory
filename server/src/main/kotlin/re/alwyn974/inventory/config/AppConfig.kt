package re.alwyn974.inventory.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {
    private val dotenv = dotenv {
        directory = "./"
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    // Database configuration
    val databaseUrl: String = dotenv["DATABASE_URL"] ?: "jdbc:h2:mem:test;MODE=PostgreSQL"
    val databaseDriver: String = dotenv["DATABASE_DRIVER"] ?: "org.h2.Driver"

    // JWT configuration
    val jwtSecret: String = dotenv["JWT_SECRET"] ?: "your-secret-key"
    val jwtIssuer: String = dotenv["JWT_ISSUER"] ?: "inventory-app"
    val jwtAudience: String = dotenv["JWT_AUDIENCE"] ?: "inventory-users"
    val jwtRealm: String = dotenv["JWT_REALM"] ?: "Inventory Application"

    // MinIO configuration
    val minioEndpoint: String = dotenv["MINIO_ENDPOINT"] ?: "http://localhost:9000"
    val minioAccessKey: String = dotenv["MINIO_ACCESS_KEY"] ?: "minioadmin"
    val minioSecretKey: String = dotenv["MINIO_SECRET_KEY"] ?: "minioadmin"
    val minioBucketName: String = dotenv["MINIO_BUCKET_NAME"] ?: "inventory-images"

    // Server configuration
    val serverPort: Int = dotenv["SERVER_PORT"]?.toIntOrNull() ?: 8080
    val serverHost: String = dotenv["SERVER_HOST"] ?: "0.0.0.0"
}
