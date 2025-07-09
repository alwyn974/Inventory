package re.alwyn974.inventory.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            url("http://192.168.1.13:8080/api/v1/")
            contentType(ContentType.Application.Json)
        }
    }

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private val refreshMutex = Mutex()
    private var isRefreshing = false

    fun setTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
    }

    fun getAccessToken(): String? = accessToken
    fun getRefreshToken(): String? = refreshToken

    private fun HttpRequestBuilder.addAuthHeader() {
        accessToken?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        val currentRefreshToken = refreshToken ?: return false

        return refreshMutex.withLock {
            if (isRefreshing) return@withLock true

            isRefreshing = true
            try {
                val response = client.post("auth/refresh") {
                    setBody(RefreshTokenRequest(currentRefreshToken))
                }

                if (response.status.isSuccess()) {
                    val refreshResponse = response.body<RefreshTokenResponse>()
                    accessToken = refreshResponse.accessToken
                    refreshToken = refreshResponse.refreshToken
                    true
                } else {
                    clearTokens()
                    false
                }
            } catch (e: Exception) {
                clearTokens()
                false
            } finally {
                isRefreshing = false
            }
        }
    }

    // Enhanced helper function with automatic token refresh
    private suspend inline fun <reified T> handleApiCall(call: suspend () -> HttpResponse): T {
        return try {
            var response = call()

            // If unauthorized, try to refresh token and retry once
            if (response.status == HttpStatusCode.Unauthorized && refreshToken != null) {
                if (refreshTokenIfNeeded()) {
                    response = call()
                }
            }

            if (response.status.isSuccess()) {
                response.body<T>()
            } else {
                val errorBody = try {
                    response.body<ErrorResponse>()
                } catch (e: Exception) {
                    ErrorResponse(
                        error = "HTTP_ERROR",
                        message = "HTTP ${response.status.value}: ${response.status.description}"
                    )
                }
                throw ApiException(errorBody)
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(
                ErrorResponse(
                    error = "NETWORK_ERROR",
                    message = e.message ?: "Unknown network error"
                )
            )
        }
    }

    // Auth endpoints
    suspend fun login(request: LoginRequest): LoginResponse {
        return handleApiCall {
            client.post("auth/login") {
                setBody(request)
            }
        }
    }

    suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        return handleApiCall {
            client.post("auth/refresh") {
                setBody(request)
            }
        }
    }

    suspend fun logout(request: RefreshTokenRequest): SuccessResponse {
        return handleApiCall {
            client.post("auth/logout") {
                setBody(request)
            }
        }
    }

    suspend fun getCurrentUser(): UserDto {
        return handleApiCall {
            client.get("auth/me") {
                addAuthHeader()
            }
        }
    }

    // Items endpoints
    suspend fun getItems(): List<ItemDto> {
        return handleApiCall {
            client.get("items") {
                addAuthHeader()
            }
        }
    }

    suspend fun getItem(id: String): ItemDto {
        return handleApiCall {
            client.get("items/$id") {
                addAuthHeader()
            }
        }
    }

    suspend fun createItem(request: CreateItemRequest): Map<String, String> {
        return handleApiCall {
            client.post("items") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun updateItem(id: String, request: UpdateItemRequest): SuccessResponse {
        return handleApiCall {
            client.patch("items/$id") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun deleteItem(id: String): SuccessResponse {
        return handleApiCall {
            client.delete("items/$id") {
                addAuthHeader()
            }
        }
    }

    suspend fun uploadItemImage(id: String, imageData: ByteArray, fileName: String): Map<String, String> {
        return handleApiCall {
            client.post("items/$id/image") {
                addAuthHeader()
                setBody(MultiPartFormDataContent(
                    formData {
                        append("image", imageData, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }
        }
    }

    // Categories endpoints
    suspend fun getCategories(): List<CategoryDto> {
        return handleApiCall {
            client.get("categories") {
                addAuthHeader()
            }
        }
    }

    suspend fun getCategory(id: String): CategoryDto {
        return handleApiCall {
            client.get("categories/$id") {
                addAuthHeader()
            }
        }
    }

    suspend fun createCategory(request: CreateCategoryRequest): Map<String, String> {
        return handleApiCall {
            client.post("categories") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun updateCategory(id: String, request: UpdateCategoryRequest): SuccessResponse {
        return handleApiCall {
            client.patch("categories/$id") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun deleteCategory(id: String): SuccessResponse {
        return handleApiCall {
            client.delete("categories/$id") {
                addAuthHeader()
            }
        }
    }

    // Tags endpoints
    suspend fun getTags(): List<TagDto> {
        return handleApiCall {
            client.get("tags") {
                addAuthHeader()
            }
        }
    }

    suspend fun getTag(id: String): TagDto {
        return handleApiCall {
            client.get("tags/$id") {
                addAuthHeader()
            }
        }
    }

    suspend fun createTag(request: CreateTagRequest): Map<String, String> {
        return handleApiCall {
            client.post("tags") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun updateTag(id: String, request: CreateTagRequest): SuccessResponse {
        return handleApiCall {
            client.patch("tags/$id") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun deleteTag(id: String): SuccessResponse {
        return handleApiCall {
            client.delete("tags/$id") {
                addAuthHeader()
            }
        }
    }

    // Folders endpoints
    suspend fun getFolders(): List<FolderDto> {
        return handleApiCall {
            client.get("folders") {
                addAuthHeader()
            }
        }
    }

    suspend fun getFolder(id: String): FolderDto {
        return handleApiCall {
            client.get("folders/$id") {
                addAuthHeader()
            }
        }
    }

    suspend fun createFolder(request: CreateFolderRequest): Map<String, String> {
        return handleApiCall {
            client.post("folders") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun updateFolder(id: String, request: UpdateFolderRequest): SuccessResponse {
        return handleApiCall {
            client.patch("folders/$id") {
                addAuthHeader()
                setBody(request)
            }
        }
    }

    suspend fun deleteFolder(id: String): SuccessResponse {
        return handleApiCall {
            client.delete("folders/$id") {
                addAuthHeader()
            }
        }
    }
}

// Custom exception class for API errors
class ApiException(val errorResponse: ErrorResponse) : Exception(errorResponse.message) {
    override fun toString(): String {
        return "ApiException(error=${errorResponse.error}, message=${errorResponse.message})"
    }
}
