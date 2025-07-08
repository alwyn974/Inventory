package re.alwyn974.inventory.storage

import java.util.prefs.Preferences

actual class PlatformStorage actual constructor() {
    private val prefs = Preferences.userNodeForPackage(PlatformStorage::class.java)
    private val keyPrefix = "inventory_"

    actual fun save(key: String, value: String) {
        val prefixedKey = keyPrefix + key
        prefs.put(prefixedKey, value)
        prefs.flush()
    }

    actual fun load(key: String): String? {
        val prefixedKey = keyPrefix + key
        return prefs.get(prefixedKey, null)
    }

    actual fun remove(key: String) {
        val prefixedKey = keyPrefix + key
        prefs.remove(prefixedKey)
        prefs.flush()
    }

    actual fun clear() {
        try {
            val keys = prefs.keys()
            keys.filter { it.startsWith(keyPrefix) }.forEach { key ->
                prefs.remove(key)
            }
            prefs.flush()
        } catch (e: Exception) {
            println("PlatformStorage: Error clearing preferences: ${e.message}")
        }
    }
}
