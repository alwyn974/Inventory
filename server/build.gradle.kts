plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
    kotlin("plugin.serialization") version "2.2.0"
}

group = "re.alwyn974.inventory"
version = "1.0.0"

application {
    mainClass.set("re.alwyn974.inventory.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.compression)

    // Content negotiation and serialization
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)

    // Authentication and authorization
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.auth.jwt.jvm)

    // CORS
    implementation(libs.ktor.server.cors.jvm)

    // Status pages for error handling
    implementation(libs.ktor.server.status.pages.jvm)

    // Call logging
    implementation(libs.ktor.server.call.logging.jvm)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)
    implementation(libs.postgresql)

    // Connection pooling
    implementation(libs.hikaricp)

    // Password hashing
    implementation(libs.jbcrypt)

    // MinIO
    implementation(libs.minio)

    // UUID
    implementation(libs.kotlinx.datetime)

    // OpenAPI/Swagger
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.swagger.codegen.generators)
    implementation(libs.ktor.openapi)

    // Koin
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Rate limiting
    implementation(libs.ktor.server.rate.limiting)
    implementation(libs.ktor.server.task.scheduling.core)
    implementation(libs.ktor.server.task.scheduling.redis)
    implementation(libs.ktor.server.task.scheduling.jdbc)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}