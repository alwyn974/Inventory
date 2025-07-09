package re.alwyn974.inventory.storage

import java.util.prefs.Preferences

actual class PlatformStorage actual constructor() {
    private val prefs = Preferences.userNodeForPackage(PlatformStorage::class.java)

    actual fun save(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun load(key: String): String? {
        return prefs.get(key, null)
    }

    actual fun remove(key: String) {
        prefs.remove(key)
    }

    actual fun clear() {
        try {
            prefs.clear()
        } catch (e: Exception) {
            // Fallback: remove known keys
            prefs.keys().forEach { key ->
                if (key.startsWith("inventory_")) {
                    prefs.remove(key)
                }
            }
        }
    }
}
