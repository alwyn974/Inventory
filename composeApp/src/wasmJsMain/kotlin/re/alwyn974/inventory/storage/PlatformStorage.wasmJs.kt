package re.alwyn974.inventory.storage

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

// Actual implementation using browser localStorage
actual class PlatformStorage actual constructor() {
    private val keyPrefix = "inventory_"

    actual fun save(key: String, value: String) {
        val prefixedKey = keyPrefix + key
        localStorage[prefixedKey] = value
    }

    actual fun load(key: String): String? {
        val prefixedKey = keyPrefix + key
        return localStorage[prefixedKey]
    }

    actual fun remove(key: String) {
        val prefixedKey = keyPrefix + key
        localStorage.removeItem(prefixedKey)
    }

    actual fun clear() {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            localStorage.key(i)?.let { key ->
                if (key.startsWith(keyPrefix))
                    keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { localStorage.removeItem(it) }
    }
}
