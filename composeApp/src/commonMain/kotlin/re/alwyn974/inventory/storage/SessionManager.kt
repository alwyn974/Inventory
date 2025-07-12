package re.alwyn974.inventory.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import re.alwyn974.inventory.shared.model.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import re.alwyn974.inventory.network.ApiClient
import kotlin.concurrent.Volatile

class SessionManager private constructor() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    private val dataStoreManager = DataStoreManager(createDataStore())
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
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

    suspend fun saveSession(accessToken: String, refreshToken: String, user: UserDto) {
        println("SessionManager: Saving session for user: ${user.username}")
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        _currentUser.value = user
        _isLoggedIn.value = true

        // Save to DataStore
        val sessionData = SessionData(accessToken, refreshToken, user)
        try {
            val sessionJson = json.encodeToString(sessionData)
            println("SessionManager: Serialized session data: ${sessionJson.take(100)}...")
            dataStoreManager.saveSession(sessionJson)
            println("SessionManager: Session saved successfully to DataStore")
        } catch (e: Exception) {
            println("SessionManager: Failed to save session: ${e.message}")
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        println("SessionManager: Updating tokens...")
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        // Update DataStore with new tokens if user exists
        _currentUser.value?.let { user ->
            saveSession(accessToken, refreshToken, user)
        }
    }

    suspend fun clearSession() {
        println("SessionManager: Clearing session...")
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _isLoggedIn.value = false

        // Clear DataStore
        dataStoreManager.clearSession()
        println("SessionManager: Session cleared from DataStore")
    }

    fun getAccessToken(): String? = accessToken
    fun getRefreshToken(): String? = refreshToken

    suspend fun loadSavedSession(): Boolean {
        return try {
            println("SessionManager: Loading saved session from DataStore...")
            val sessionJson = dataStoreManager.getSession()
            if (sessionJson != null) {
                println("SessionManager: Found saved session data: ${sessionJson}...")
                val sessionData = json.decodeFromString<SessionData>(sessionJson)
                accessToken = sessionData.accessToken
                refreshToken = sessionData.refreshToken
                _currentUser.value = sessionData.user
                _isLoggedIn.value = true
                println("SessionManager: Session restored for user: ${sessionData.user.username}")
                true
            } else {
                println("SessionManager: No saved session found in DataStore")
                false
            }
        } catch (e: Exception) {
            println("SessionManager: Failed to load session: ${e.message}")
            dataStoreManager.clearSession()
            false
        }
    }

    fun hasValidSession(): Boolean {
        val isValid = accessToken != null && refreshToken != null && _currentUser.value != null
        println("SessionManager: Has valid session: $isValid")
        return isValid
    }

    // Initialize ApiClient with saved tokens
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
