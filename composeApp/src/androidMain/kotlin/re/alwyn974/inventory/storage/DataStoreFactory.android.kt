package re.alwyn974.inventory.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = dataStoreFileName)

private var appContext: Context? = null

fun initializeDataStore(context: Context) {
    appContext = context.applicationContext
}

actual fun createDataStore(): DataStore<Preferences> {
    return appContext?.dataStore
        ?: throw IllegalStateException("DataStore not initialized. Call initializeDataStore(context) first.")
}
