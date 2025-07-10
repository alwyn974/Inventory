package re.alwyn974.inventory.storage

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

// WASM-specific implementation for localStorage operations
internal actual fun saveToLocalStorage(sessionJson: String) {
    localStorage["inventory_session"] = sessionJson
}

internal actual fun loadFromLocalStorage(): String? {
    return localStorage["inventory_session"]
}

internal actual fun clearFromLocalStorage() {
    localStorage.removeItem("inventory_session")
}
