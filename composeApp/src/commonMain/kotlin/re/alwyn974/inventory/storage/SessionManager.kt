package re.alwyn974.inventory.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import re.alwyn974.inventory.shared.model.UserDto
import kotlin.concurrent.Volatile

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
        loadSavedSession()
    }

    fun saveSession(accessToken: String, refreshToken: String, user: UserDto) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        _currentUser.value = user
        _isLoggedIn.value = true

        // Save to persistent storage
        val sessionData = SessionData(accessToken, refreshToken, user)
        try {
            val sessionJson = json.encodeToString(sessionData)
            storage.save(SESSION_KEY, sessionJson)
        } catch (e: Exception) {
            // Handle serialization error - continue without persistent storage
            println("Failed to save session: ${e.message}")
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        // Update storage with new tokens if user exists
        _currentUser.value?.let { user ->
            saveSession(accessToken, refreshToken, user)
        }
    }

    fun clearSession() {
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _isLoggedIn.value = false

        // Clear persistent storage
        storage.remove(SESSION_KEY)
    }

    fun getAccessToken(): String? = accessToken
    fun getRefreshToken(): String? = refreshToken

    private fun loadSavedSession(): Boolean {
        return try {
            val sessionJson = storage.load(SESSION_KEY)
            if (sessionJson != null) {
                val sessionData = json.decodeFromString<SessionData>(sessionJson)
                accessToken = sessionData.accessToken
                refreshToken = sessionData.refreshToken
                _currentUser.value = sessionData.user
                _isLoggedIn.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Handle deserialization error - clear corrupted data
            println("Failed to load session: ${e.message}")
            storage.remove(SESSION_KEY)
            false
        }
    }

    fun hasValidSession(): Boolean {
        return accessToken != null && refreshToken != null && _currentUser.value != null
    }

    private data class SessionData(
        val accessToken: String,
        val refreshToken: String,
        val user: UserDto
    )
}
