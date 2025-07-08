package re.alwyn974.inventory.storage

import platform.Foundation.NSUserDefaults

// Actual implementation for iOS using NSUserDefaults
actual class PlatformStorage actual constructor() {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val keyPrefix = "inventory_"

    actual fun save(key: String, value: String) {
        val prefixedKey = keyPrefix + key
        userDefaults.setObject(value, prefixedKey)
        userDefaults.synchronize()
    }

    actual fun load(key: String): String? {
        val prefixedKey = keyPrefix + key
        return userDefaults.stringForKey(prefixedKey)
    }

    actual fun remove(key: String) {
        val prefixedKey = keyPrefix + key
        userDefaults.removeObjectForKey(prefixedKey)
        userDefaults.synchronize()
    }

    actual fun clear() {
        val dictionary = userDefaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            if (key is String && key.startsWith(keyPrefix)) {
                userDefaults.removeObjectForKey(key)
            }
        }
        userDefaults.synchronize()
    }
}
