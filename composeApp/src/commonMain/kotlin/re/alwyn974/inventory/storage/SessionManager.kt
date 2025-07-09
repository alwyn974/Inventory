package re.alwyn974.inventory.storage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import re.alwyn974.inventory.shared.model.UserDto
import kotlin.jvm.JvmStatic

class SessionManager {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    fun saveSession(accessToken: String, refreshToken: String, user: UserDto) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        _currentUser.value = user
        _isLoggedIn.value = true

        // Save to local storage (implementation depends on platform)
        saveToLocalStorage(accessToken, refreshToken, user)
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        // Update local storage
        saveTokensToLocalStorage(accessToken, refreshToken)
    }

    fun clearSession() {
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _isLoggedIn.value = false

        // Clear local storage
        clearLocalStorage()
    }

    fun getAccessToken(): String? = accessToken
    fun getRefreshToken(): String? = refreshToken

    fun loadSavedSession(): Boolean {
        val savedTokens = loadFromLocalStorage()
        return if (savedTokens != null) {
            accessToken = savedTokens.accessToken
            refreshToken = savedTokens.refreshToken
            _currentUser.value = savedTokens.user
            _isLoggedIn.value = true
            true
        } else {
            false
        }
    }

    // Platform-specific implementations
    private fun saveToLocalStorage(accessToken: String, refreshToken: String, user: UserDto) {
        // TODO: Implement platform-specific storage (SharedPreferences for Android, UserDefaults for iOS, localStorage for Web)
        // For now, we'll just keep tokens in memory
    }

    private fun saveTokensToLocalStorage(accessToken: String, refreshToken: String) {
        // TODO: Implement platform-specific storage
    }

    private fun clearLocalStorage() {
        // TODO: Implement platform-specific storage clearing
    }

    private fun loadFromLocalStorage(): SavedSession? {
        // TODO: Implement platform-specific loading
        return null
    }

    private data class SavedSession(
        val accessToken: String,
        val refreshToken: String,
        val user: UserDto
    )

    companion object {
        @JvmStatic
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }
}
