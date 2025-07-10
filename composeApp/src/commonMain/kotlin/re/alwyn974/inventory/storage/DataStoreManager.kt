package re.alwyn974.inventory.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreManager(private val dataStore: DataStore<Preferences>?) {

    companion object {
        private val SESSION_KEY = stringPreferencesKey("inventory_session")
    }

    suspend fun saveSession(sessionJson: String) {
        if (dataStore != null) {
            // Use DataStore for platforms that support it
            dataStore.edit { preferences ->
                preferences[SESSION_KEY] = sessionJson
            }
        } else {
            // Fallback for WASM - use direct localStorage access
            saveToLocalStorage(sessionJson)
        }
    }

    suspend fun getSession(): String? {
        return if (dataStore != null) {
            // Use DataStore for platforms that support it
            dataStore.data.map { preferences ->
                preferences[SESSION_KEY]
            }.first()
        } else {
            // Fallback for WASM - use direct localStorage access
            loadFromLocalStorage()
        }
    }

    suspend fun clearSession() {
        if (dataStore != null) {
            // Use DataStore for platforms that support it
            dataStore.edit { preferences ->
                preferences.remove(SESSION_KEY)
            }
        } else {
            // Fallback for WASM - use direct localStorage access
            clearFromLocalStorage()
        }
    }
}

// Platform-specific localStorage implementations
internal expect fun saveToLocalStorage(sessionJson: String)
internal expect fun loadFromLocalStorage(): String?
internal expect fun clearFromLocalStorage()
