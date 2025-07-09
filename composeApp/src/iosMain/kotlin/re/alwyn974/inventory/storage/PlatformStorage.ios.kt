package re.alwyn974.inventory.storage

import platform.Foundation.NSUserDefaults

actual class PlatformStorage actual constructor() {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun save(key: String, value: String) {
        userDefaults.setObject(value, key)
    }

    actual fun load(key: String): String? {
        return userDefaults.stringForKey(key)
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        // Clear all app-specific keys (you might want to be more selective)
        val dictionary = userDefaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            if (key is String && key.startsWith("inventory_")) {
                userDefaults.removeObjectForKey(key)
            }
        }
    }
}
