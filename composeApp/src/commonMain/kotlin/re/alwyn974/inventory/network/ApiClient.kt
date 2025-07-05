package re.alwyn974.inventory.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import re.alwyn974.inventory.shared.model.*

class ApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        install(DefaultRequest) {
            url("http://localhost:8080/api/v1/")
            contentType(ContentType.Application.Json)
        }
    }

    private var authToken: String? = null

    fun setAuthToken(token: String) {
        authToken = token
    }

    private fun HttpRequestBuilder.addAuthHeader() {
        authToken?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }

    // Auth endpoints
    suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("auth/login") {
            setBody(request)
        }.body()
    }

    suspend fun getCurrentUser(): UserDto {
        return client.get("auth/me") {
            addAuthHeader()
        }.body()
    }

    // Items endpoints
    suspend fun getItems(): List<ItemDto> {
        return client.get("items") {
            addAuthHeader()
        }.body()
    }

    suspend fun getItem(id: String): ItemDto {
        return client.get("items/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun createItem(request: CreateItemRequest): Map<String, String> {
        return client.post("items") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateItem(id: String, request: UpdateItemRequest): SuccessResponse {
        return client.patch("items/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteItem(id: String): SuccessResponse {
        return client.delete("items/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun uploadItemImage(id: String, imageData: ByteArray, fileName: String): Map<String, String> {
        return client.post("items/$id/image") {
            addAuthHeader()
            setBody(MultiPartFormDataContent(
                formData {
                    append("image", imageData, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            ))
        }.body()
    }

    // Categories endpoints
    suspend fun getCategories(): List<CategoryDto> {
        return client.get("categories") {
            addAuthHeader()
        }.body()
    }

    suspend fun getCategory(id: String): CategoryDto {
        return client.get("categories/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun createCategory(request: CreateCategoryRequest): Map<String, String> {
        return client.post("categories") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateCategory(id: String, request: UpdateCategoryRequest): SuccessResponse {
        return client.patch("categories/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteCategory(id: String): SuccessResponse {
        return client.delete("categories/$id") {
            addAuthHeader()
        }.body()
    }

    // Tags endpoints
    suspend fun getTags(): List<TagDto> {
        return client.get("tags") {
            addAuthHeader()
        }.body()
    }

    suspend fun getTag(id: String): TagDto {
        return client.get("tags/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun createTag(request: CreateTagRequest): Map<String, String> {
        return client.post("tags") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateTag(id: String, request: CreateTagRequest): SuccessResponse {
        return client.patch("tags/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteTag(id: String): SuccessResponse {
        return client.delete("tags/$id") {
            addAuthHeader()
        }.body()
    }

    // Folders endpoints
    suspend fun getFolders(): List<FolderDto> {
        return client.get("folders") {
            addAuthHeader()
        }.body()
    }

    suspend fun getFolder(id: String): FolderDto {
        return client.get("folders/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun createFolder(request: CreateFolderRequest): Map<String, String> {
        return client.post("folders") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateFolder(id: String, request: UpdateFolderRequest): SuccessResponse {
        return client.patch("folders/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteFolder(id: String): SuccessResponse {
        return client.delete("folders/$id") {
            addAuthHeader()
        }.body()
    }
}
