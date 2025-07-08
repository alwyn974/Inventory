package re.alwyn974.inventory.storage

import android.content.Context
import android.content.SharedPreferences

actual class PlatformStorage actual constructor() {
    private var sharedPreferences: SharedPreferences? = null

    companion object {
        private const val PREFS_NAME = "inventory_storage"
        private var applicationContext: Context? = null

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }
    }

    private fun getPreferences(): SharedPreferences? {
        if (sharedPreferences == null && applicationContext != null) {
            sharedPreferences = applicationContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return sharedPreferences
    }

    actual fun save(key: String, value: String) {
        getPreferences()?.edit()?.putString(key, value)?.apply()
            ?: println("PlatformStorage: Warning - Could not save $key, SharedPreferences not initialized")
    }

    actual fun load(key: String): String? {
        return getPreferences()?.getString(key, null)
            ?: run {
                println("PlatformStorage: Warning - Could not load $key, SharedPreferences not initialized")
                null
            }
    }

    actual fun remove(key: String) {
        getPreferences()?.edit()?.remove(key)?.apply()
            ?: println("PlatformStorage: Warning - Could not remove $key, SharedPreferences not initialized")
    }

    actual fun clear() {
        getPreferences()?.edit()?.clear()?.apply()
            ?: println("PlatformStorage: Warning - Could not clear storage, SharedPreferences not initialized")
    }
}
