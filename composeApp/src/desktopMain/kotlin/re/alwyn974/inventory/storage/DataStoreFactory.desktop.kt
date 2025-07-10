package re.alwyn974.inventory.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences>? {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val userHome = System.getProperty("user.home")
            val appDataDir = File(userHome, ".inventory")
            if (!appDataDir.exists()) {
                appDataDir.mkdirs()
            }
            File(appDataDir, "inventory_preferences.preferences_pb").absolutePath.toPath()
        }
    )
}
