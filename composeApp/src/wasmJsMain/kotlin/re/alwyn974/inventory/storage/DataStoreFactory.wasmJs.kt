package re.alwyn974.inventory.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

// WASM/JS doesn't support DataStore, so we return null and use localStorage fallback
actual fun createDataStore(): DataStore<Preferences>? {
    return null
}
