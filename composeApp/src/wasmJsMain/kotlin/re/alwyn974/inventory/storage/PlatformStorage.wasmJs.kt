package re.alwyn974.inventory.storage

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

actual class PlatformStorage actual constructor() {

    actual fun save(key: String, value: String) {
        localStorage[key] = value
    }

    actual fun load(key: String): String? {
        return localStorage[key]
    }

    actual fun remove(key: String) {
        localStorage.removeItem(key)
    }

    actual fun clear() {
        // Clear all app-specific keys
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            localStorage.key(i)?.let { key ->
                if (key.startsWith("inventory_")) {
                    keysToRemove.add(key)
                }
            }
        }
        keysToRemove.forEach { localStorage.removeItem(it) }
    }
}
