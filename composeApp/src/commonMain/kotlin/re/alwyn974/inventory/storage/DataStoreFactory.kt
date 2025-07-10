package re.alwyn974.inventory.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
expect fun createDataStore(): DataStore<Preferences>?

internal const val dataStoreFileName = "inventory.preferences"