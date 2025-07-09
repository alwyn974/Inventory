package re.alwyn974.inventory.storage

import android.content.Context
import android.content.SharedPreferences

actual class PlatformStorage actual constructor() {
    private val sharedPreferences: SharedPreferences by lazy {
        // Note: Context should be injected in a real implementation
        // For now, we'll use a simple in-memory storage as fallback
        throw NotImplementedError("Android Context not available. Use PlatformStorage(context) constructor.")
    }

    constructor(context: Context) : this() {
        // This would be the proper constructor for Android
    }

    private val memoryStorage = mutableMapOf<String, String>()

    actual fun save(key: String, value: String) {
        try {
            sharedPreferences.edit().putString(key, value).apply()
        } catch (e: Exception) {
            // Fallback to memory storage
            memoryStorage[key] = value
        }
    }

    actual fun load(key: String): String? {
        return try {
            sharedPreferences.getString(key, null)
        } catch (e: Exception) {
            memoryStorage[key]
        }
    }

    actual fun remove(key: String) {
        try {
            sharedPreferences.edit().remove(key).apply()
        } catch (e: Exception) {
            memoryStorage.remove(key)
        }
    }

    actual fun clear() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            memoryStorage.clear()
        }
    }
}
