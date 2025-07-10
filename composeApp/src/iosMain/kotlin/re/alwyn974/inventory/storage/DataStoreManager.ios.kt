package re.alwyn974.inventory.storage

// iOS uses DataStore, so these localStorage functions should never be called
internal actual fun saveToLocalStorage(sessionJson: String) {
    throw UnsupportedOperationException("localStorage not used on iOS - DataStore is available")
}

internal actual fun loadFromLocalStorage(): String? {
    throw UnsupportedOperationException("localStorage not used on iOS - DataStore is available")
}

internal actual fun clearFromLocalStorage() {
    throw UnsupportedOperationException("localStorage not used on iOS - DataStore is available")
}
