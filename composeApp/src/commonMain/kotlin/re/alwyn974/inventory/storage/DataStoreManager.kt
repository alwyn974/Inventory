package re.alwyn974.inventory.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val SESSION_KEY = stringPreferencesKey("inventory_session")
    }

    suspend fun saveSession(sessionJson: String) {
        dataStore.edit { preferences ->
            preferences[SESSION_KEY] = sessionJson
        }
    }

    suspend fun getSession(): String? {
        return dataStore.data.map { preferences ->
            preferences[SESSION_KEY]
        }.first()
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_KEY)
        }
    }
}
