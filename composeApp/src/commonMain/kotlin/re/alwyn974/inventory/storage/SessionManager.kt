package re.alwyn974.inventory.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import re.alwyn974.inventory.shared.model.UserDto
import kotlin.concurrent.Volatile
import kotlinx.serialization.Serializable
import re.alwyn974.inventory.network.ApiClient

class SessionManager {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    private val storage = PlatformStorage()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val SESSION_KEY = "inventory_session"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: run {
                val newInstance = SessionManager()
                instance = newInstance
                newInstance
            }
        }
    }

    init {
        // Try to load saved session on initialization
        println("SessionManager: Initializing...")
        val loaded = loadSavedSession()
        println("SessionManager: Session loaded: $loaded")
    }

    fun saveSession(accessToken: String, refreshToken: String, user: UserDto) {
        println("SessionManager: Saving session for user: ${user.username}")
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        _currentUser.value = user
        _isLoggedIn.value = true

        // Save to persistent storage
        val sessionData = SessionData(accessToken, refreshToken, user)
        try {
            val sessionJson = json.encodeToString(sessionData)
            println("SessionManager: Serialized session data: ${sessionJson.take(100)}...")
            storage.save(SESSION_KEY, sessionJson)
            println("SessionManager: Session saved successfully")
        } catch (e: Exception) {
            // Handle serialization error - continue without persistent storage
            println("SessionManager: Failed to save session: ${e.message}")
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        println("SessionManager: Updating tokens...")
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        // Update storage with new tokens if user exists
        _currentUser.value?.let { user ->
            saveSession(accessToken, refreshToken, user)
        }
    }

    fun clearSession() {
        println("SessionManager: Clearing session...")
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _isLoggedIn.value = false

        // Clear persistent storage
        storage.remove(SESSION_KEY)
        println("SessionManager: Session cleared")
    }

    fun getAccessToken(): String? = accessToken
    fun getRefreshToken(): String? = refreshToken

    private fun loadSavedSession(): Boolean {
        return try {
            println("SessionManager: Loading saved session...")
            val sessionJson = storage.load(SESSION_KEY)
            if (sessionJson != null) {
                println("SessionManager: Found saved session data: ${sessionJson.take(100)}...")
                val sessionData = json.decodeFromString<SessionData>(sessionJson)
                accessToken = sessionData.accessToken
                refreshToken = sessionData.refreshToken
                _currentUser.value = sessionData.user
                _isLoggedIn.value = true
                println("SessionManager: Session restored for user: ${sessionData.user.username}")
                true
            } else {
                println("SessionManager: No saved session found")
                false
            }
        } catch (e: Exception) {
            // Handle deserialization error - clear corrupted data
            println("SessionManager: Failed to load session: ${e.message}")
            storage.remove(SESSION_KEY)
            false
        }
    }

    fun hasValidSession(): Boolean {
        val isValid = accessToken != null && refreshToken != null && _currentUser.value != null
        println("SessionManager: Has valid session: $isValid")
        return isValid
    }

    // New method to initialize ApiClient with saved tokens
    fun initializeApiClient(apiClient: ApiClient) {
        if (hasValidSession()) {
            println("SessionManager: Initializing ApiClient with saved tokens")
            apiClient.setTokens(accessToken!!, refreshToken!!)
        } else {
            println("SessionManager: No valid session to initialize ApiClient")
        }
    }

    @Serializable
    private data class SessionData(
        val accessToken: String,
        val refreshToken: String,
        val user: UserDto
    )
}
